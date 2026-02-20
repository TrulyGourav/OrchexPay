package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.exceptions.LedgerEntryNotFoundException;
import com.orchexpay.walletledger.repositories.LedgerEntryRepository;
import com.orchexpay.walletledger.models.LedgerEntry;
import com.orchexpay.walletledger.utils.LedgerEntryFactory;
import com.orchexpay.walletledger.enums.EntryStatus;
import com.orchexpay.walletledger.enums.ReferenceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Reverses a PENDING payout reservation on bank failure: marks original REVERSED and creates
 * a compensating CONFIRMED CREDIT (reference_type=REVERSAL) so balance is restored.
 * Idempotent: if original already REVERSED, returns existing reversal entry or no-op.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReverseReservationUseCase {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerEntryFactory ledgerEntryFactory;

    @Transactional
    public LedgerEntry execute(UUID pendingEntryId) {
        LedgerEntry pending = ledgerEntryRepository.findById(pendingEntryId)
                .orElseThrow(() -> new LedgerEntryNotFoundException(pendingEntryId));
        if (pending.getStatus() == EntryStatus.REVERSED) {
            log.info("Idempotent reverse: entry {} already REVERSED", pendingEntryId);
            var reversalRef = pending.getReferenceId() + "-reversal";
            return ledgerEntryRepository.findByWalletIdAndReferenceIdAndReferenceType(
                            pending.getWalletId(), reversalRef, ReferenceType.REVERSAL)
                    .orElse(pending);
        }
        if (pending.getStatus() != EntryStatus.PENDING) {
            throw new IllegalStateException("Only PENDING entries can be reversed; entry " + pendingEntryId + " has status " + pending.getStatus());
        }
        String reversalRefId = pending.getReferenceId() + "-reversal";
        var existingReversal = ledgerEntryRepository.findByWalletIdAndReferenceIdAndReferenceType(
                pending.getWalletId(), reversalRefId, ReferenceType.REVERSAL);
        if (existingReversal.isPresent()) {
            pending.setStatus(EntryStatus.REVERSED);
            ledgerEntryRepository.save(pending);
            return existingReversal.get();
        }
        LedgerEntry compensatingCredit = ledgerEntryFactory.createCredit(
                pending.getWalletId(), pending.getMerchantId(), pending.getVendorId(),
                pending.getAmount(), ReferenceType.REVERSAL, reversalRefId,
                EntryStatus.CONFIRMED, "Reversal of " + pending.getReferenceId());
        compensatingCredit = ledgerEntryRepository.save(compensatingCredit);
        pending.setStatus(EntryStatus.REVERSED);
        ledgerEntryRepository.save(pending);
        log.info("Reversed reservation entry {} with compensating credit {}", pendingEntryId, compensatingCredit.getId());
        return compensatingCredit;
    }
}
