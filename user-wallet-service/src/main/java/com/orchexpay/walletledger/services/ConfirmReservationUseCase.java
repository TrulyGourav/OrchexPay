package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.exceptions.LedgerEntryNotFoundException;
import com.orchexpay.walletledger.repositories.LedgerEntryRepository;
import com.orchexpay.walletledger.models.LedgerEntry;
import com.orchexpay.walletledger.enums.EntryStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Confirms a PENDING ledger entry (e.g. payout reserve) after bank success.
 * Transition: PENDING → CONFIRMED. Idempotent: if already CONFIRMED, no-op.
 * Only PENDING entries can be confirmed.
 * This is the only allowed in-place mutation on ledger entries (status only); amount/type/reference are never changed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfirmReservationUseCase {

    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional
    public LedgerEntry execute(UUID entryId) {
        LedgerEntry entry = ledgerEntryRepository.findById(entryId)
                .orElseThrow(() -> new LedgerEntryNotFoundException(entryId));
        if (entry.getStatus() == EntryStatus.CONFIRMED) {
            log.info("Idempotent confirm: entry {} already CONFIRMED", entryId);
            return entry;
        }
        if (entry.getStatus() != EntryStatus.PENDING) {
            throw new IllegalStateException("Only PENDING entries can be confirmed; entry " + entryId + " has status " + entry.getStatus());
        }
        entry.setStatus(EntryStatus.CONFIRMED);
        entry = ledgerEntryRepository.save(entry);
        log.info("Confirmed reservation entry {}", entryId);
        return entry;
    }
}
