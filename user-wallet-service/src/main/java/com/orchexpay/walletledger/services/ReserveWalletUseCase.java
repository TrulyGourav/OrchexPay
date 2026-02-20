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
import java.util.UUID;

/**
 * Reserves funds by creating a PENDING DEBIT (reference_type=PAYOUT).
 * Balance is unchanged until confirm (PENDING→CONFIRMED) or reverse (compensating CREDIT).
 * Idempotent by (walletId, referenceId, referenceType). Fails if confirmed balance &lt; amount.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReserveWalletUseCase {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerEntryFactory ledgerEntryFactory;

    @Transactional
    public LedgerEntry execute(UUID walletId, Money amount, String referenceId, String description) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
        if (!wallet.isActive()) {
            throw new IllegalStateException("Wallet is not active: " + walletId);
        }
        if (!wallet.getCurrency().equals(amount.getCurrency())) {
            throw new IllegalArgumentException("Currency mismatch");
        }
        BigDecimal confirmedBalance = ledgerEntryRepository.computeBalance(walletId);
        if (confirmedBalance.compareTo(amount.getAmount()) < 0) {
            throw new InsufficientBalanceException(walletId);
        }
        var existing = ledgerEntryRepository.findByWalletIdAndReferenceIdAndReferenceType(
                walletId, referenceId, ReferenceType.PAYOUT);
        if (existing.isPresent()) {
            if (existing.get().getStatus() == EntryStatus.PENDING) {
                log.info("Idempotent reserve: wallet {} ref {} already has PENDING debit", walletId, referenceId);
                return existing.get();
            }
            throw new IllegalStateException("Payout reference already settled or reversed: " + referenceId);
        }
        LedgerEntry entry = ledgerEntryFactory.createDebit(
                walletId, wallet.getMerchantId(), wallet.getVendorUserId(),
                amount, ReferenceType.PAYOUT, referenceId, EntryStatus.PENDING, description);
        entry = ledgerEntryRepository.save(entry);
        log.info("Reserved wallet {} amount {} ref {}", walletId, amount, referenceId);
        return entry;
    }
}
