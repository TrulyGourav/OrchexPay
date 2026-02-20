package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.exceptions.WalletNotFoundException;
import com.orchexpay.walletledger.events.DomainEventPublisher;
import com.orchexpay.walletledger.repositories.LedgerEntryRepository;
import com.orchexpay.walletledger.repositories.WalletRepository;
import com.orchexpay.walletledger.events.WalletCreditedEvent;
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

import java.time.Instant;
import java.util.UUID;

/**
 * Credits a wallet with a CONFIRMED entry. Idempotent by (walletId, referenceId, referenceType).
 * Used for: payment success (ORDER), transfer target (ORDER), reversal (REVERSAL).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditWalletUseCase {

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
        ReferenceType refType = referenceType != null ? referenceType : ReferenceType.ORDER;
        var existing = ledgerEntryRepository.findByWalletIdAndReferenceIdAndReferenceType(walletId, referenceId, refType);
        if (existing.isPresent()) {
            log.info("Idempotent credit: wallet {} ref {} type {} already exists", walletId, referenceId, refType);
            return existing.get();
        }
        LedgerEntry entry = ledgerEntryFactory.createCredit(
                walletId, wallet.getMerchantId(), wallet.getVendorUserId(),
                amount, refType, referenceId, EntryStatus.CONFIRMED, description);
        entry = ledgerEntryRepository.save(entry);
        eventPublisher.publish(WalletCreditedEvent.builder()
                .eventId(UUID.randomUUID())
                .walletId(walletId)
                .amount(amount.getAmount())
                .currencyCode(amount.getCurrency().getCode())
                .referenceId(referenceId)
                .occurredAt(Instant.now())
                .correlationId(null)
                .build());
        log.info("Credited wallet {} amount {} ref {} type {}", walletId, amount, referenceId, refType);
        return entry;
    }
}
