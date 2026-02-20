package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.exceptions.InsufficientBalanceException;
import com.orchexpay.walletledger.exceptions.WalletNotFoundException;
import com.orchexpay.walletledger.events.DomainEventPublisher;
import com.orchexpay.walletledger.repositories.LedgerEntryRepository;
import com.orchexpay.walletledger.repositories.WalletRepository;
import com.orchexpay.walletledger.events.WalletDebitedEvent;
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
import java.time.Instant;
import java.util.UUID;

/**
 * Debits a wallet with a CONFIRMED entry. Fails if confirmed balance would go negative.
 * For payout flow use ReserveWalletUseCase (PENDING) then Confirm or Reverse.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DebitWalletUseCase {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerEntryFactory ledgerEntryFactory;
    private final DomainEventPublisher eventPublisher;

    @Transactional
    public LedgerEntry execute(UUID walletId, Money amount, ReferenceType referenceType, String referenceId, String description) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
        if (!wallet.isActive()) {
            throw new IllegalStateException("Wallet is not active: " + walletId);
        }
        if (!wallet.getCurrency().equals(amount.getCurrency())) {
            throw new IllegalArgumentException("Currency mismatch");
        }
        BigDecimal currentBalance = ledgerEntryRepository.computeBalance(walletId);
        if (currentBalance.compareTo(amount.getAmount()) < 0) {
            throw new InsufficientBalanceException(walletId);
        }
        ReferenceType refType = referenceType != null ? referenceType : ReferenceType.ORDER;
        LedgerEntry entry = ledgerEntryFactory.createDebit(
                walletId, wallet.getMerchantId(), wallet.getVendorUserId(),
                amount, refType, referenceId, EntryStatus.CONFIRMED, description);
        entry = ledgerEntryRepository.save(entry);
        eventPublisher.publish(WalletDebitedEvent.builder()
                .eventId(UUID.randomUUID())
                .walletId(walletId)
                .amount(amount.getAmount())
                .currencyCode(amount.getCurrency().getCode())
                .referenceId(referenceId)
                .occurredAt(Instant.now())
                .correlationId(null)
                .build());
        log.info("Debited wallet {} amount {} ref {} type {}", walletId, amount, referenceId, refType);
        return entry;
    }
}
