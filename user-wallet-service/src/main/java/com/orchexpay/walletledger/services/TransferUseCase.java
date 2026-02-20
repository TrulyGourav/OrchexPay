package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.exceptions.InsufficientBalanceException;
import com.orchexpay.walletledger.exceptions.WalletNotFoundException;
import com.orchexpay.walletledger.repositories.LedgerEntryRepository;
import com.orchexpay.walletledger.repositories.WalletRepository;
import com.orchexpay.walletledger.models.LedgerEntry;
import com.orchexpay.walletledger.utils.LedgerEntryFactory;
import com.orchexpay.walletledger.models.Wallet;
import com.orchexpay.walletledger.enums.EntryStatus;
import com.orchexpay.walletledger.models.Money;
import com.orchexpay.walletledger.enums.ReferenceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Atomic transfer: one DEBIT from source wallet, N CREDITS to destination wallets (e.g. ESCROW → VENDOR + MAIN).
 * All entries CONFIRMED, reference_type=ORDER. Idempotent by (fromWalletId, referenceId): if debit already exists, no-op.
 * Used for order completion split: ESCROW debit total, VENDOR credit vendorShare, MAIN credit platformShare.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransferUseCase {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerEntryFactory ledgerEntryFactory;

    /**
     * @param fromWalletId   source wallet (e.g. ESCROW)
     * @param referenceId    idempotency key (e.g. order-001-split)
     * @param currencyCode   currency for amounts
     * @param totalDebit     amount to debit from source (must equal sum of creditAmounts)
     * @param credits       list of (toWalletId, amount) for each destination
     */
    @Transactional
    public TransferResult execute(UUID fromWalletId, String referenceId, String currencyCode,
                                  BigDecimal totalDebit, List<CreditLeg> credits, String description) {
        Wallet fromWallet = walletRepository.findById(fromWalletId)
                .orElseThrow(() -> new WalletNotFoundException(fromWalletId));
        if (!fromWallet.isActive()) {
            throw new IllegalStateException("Source wallet is not active: " + fromWalletId);
        }
        BigDecimal sumCredits = credits.stream().map(CreditLeg::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDebit.compareTo(sumCredits) != 0) {
            throw new IllegalArgumentException("Total debit must equal sum of credit amounts");
        }
        var existingDebit = ledgerEntryRepository.findByWalletIdAndReferenceIdAndReferenceType(
                fromWalletId, referenceId, ReferenceType.ORDER);
        if (existingDebit.isPresent() && existingDebit.get().getType() == com.orchexpay.walletledger.enums.EntryType.DEBIT) {
            log.info("Idempotent transfer: fromWallet {} ref {} already debited", fromWalletId, referenceId);
            return TransferResult.idempotent();
        }
        BigDecimal confirmedBalance = ledgerEntryRepository.computeBalance(fromWalletId);
        if (confirmedBalance.compareTo(totalDebit) < 0) {
            throw new InsufficientBalanceException(fromWalletId);
        }
        Money totalMoney = Money.of(totalDebit, currencyCode);
        LedgerEntry debitEntry = ledgerEntryFactory.createDebit(
                fromWalletId, fromWallet.getMerchantId(), fromWallet.getVendorUserId(),
                totalMoney, ReferenceType.ORDER, referenceId, EntryStatus.CONFIRMED,
                description != null ? description : "Transfer " + referenceId);
        debitEntry = ledgerEntryRepository.save(debitEntry);
        List<LedgerEntry> creditEntries = new ArrayList<>();
        for (CreditLeg leg : credits) {
            Wallet toWallet = walletRepository.findById(leg.toWalletId())
                    .orElseThrow(() -> new WalletNotFoundException(leg.toWalletId()));
            if (!toWallet.isActive()) {
                throw new IllegalStateException("Destination wallet is not active: " + leg.toWalletId());
            }
            Money legAmount = Money.of(leg.amount(), currencyCode);
            LedgerEntry creditEntry = ledgerEntryFactory.createCredit(
                    leg.toWalletId(), toWallet.getMerchantId(), toWallet.getVendorUserId(),
                    legAmount, ReferenceType.ORDER, referenceId, EntryStatus.CONFIRMED,
                    description != null ? description : "Transfer " + referenceId);
            creditEntry = ledgerEntryRepository.save(creditEntry);
            creditEntries.add(creditEntry);
        }
        log.info("Transfer from {} ref {} total {} to {} legs", fromWalletId, referenceId, totalDebit, credits.size());
        return new TransferResult(debitEntry, creditEntries, false);
    }

    public record CreditLeg(UUID toWalletId, BigDecimal amount) {}

    public record TransferResult(LedgerEntry debitEntry, List<LedgerEntry> creditEntries, boolean reused) {
        public static TransferResult idempotent() {
            return new TransferResult(null, List.of(), true);
        }
    }
}
