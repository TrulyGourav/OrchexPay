package com.orchexpay.walletledger.controllers;

import com.orchexpay.walletledger.dtos.LedgerEntryResponse;
import com.orchexpay.walletledger.mappers.LedgerEntryMapper;
import com.orchexpay.walletledger.configs.IdempotencyStore;
import com.orchexpay.walletledger.services.ConfirmReservationUseCase;
import com.orchexpay.walletledger.services.ReverseReservationUseCase;
import com.orchexpay.walletledger.models.LedgerEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

/**
 * Confirm/reverse ledger reservations. Used by payout-orchestrator after bank success/failure.
 * Does NOT create entries; only transitions PENDING → CONFIRMED or creates compensating REVERSAL.
 */
@RestController
@RequestMapping("/api/v1/ledger-entries")
@RequiredArgsConstructor
public class LedgerController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final long IDEMPOTENCY_TTL_SECONDS = 86400;

    private final ConfirmReservationUseCase confirmReservationUseCase;
    private final ReverseReservationUseCase reverseReservationUseCase;
    private final LedgerEntryMapper ledgerEntryMapper;
    private final IdempotencyStore idempotencyStore;

    /**
     * Confirm a PENDING entry (e.g. payout reserve) after bank success. PENDING → CONFIRMED.
     */
    @PostMapping("/{entryId}/confirm")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN', 'SYSTEM')")
    public ResponseEntity<LedgerEntryResponse> confirm(
            @PathVariable UUID entryId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = true) String idempotencyKey) {
        Optional<String> cached = idempotencyStore.getIfPresent(idempotencyKey);
        if (cached.isPresent()) {
            return ResponseEntity.ok(ledgerEntryMapper.toResponseFromJson(cached.get()));
        }
        LedgerEntry entry = confirmReservationUseCase.execute(entryId);
        LedgerEntryResponse response = ledgerEntryMapper.toResponse(entry);
        idempotencyStore.put(idempotencyKey, ledgerEntryMapper.toJson(response), IDEMPOTENCY_TTL_SECONDS);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Reverse a PENDING entry on bank failure. Marks original REVERSED and creates compensating CREDIT.
     */
    @PostMapping("/{entryId}/reverse")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN', 'SYSTEM')")
    public ResponseEntity<LedgerEntryResponse> reverse(
            @PathVariable UUID entryId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = true) String idempotencyKey) {
        Optional<String> cached = idempotencyStore.getIfPresent(idempotencyKey);
        if (cached.isPresent()) {
            return ResponseEntity.ok(ledgerEntryMapper.toResponseFromJson(cached.get()));
        }
        LedgerEntry compensatingEntry = reverseReservationUseCase.execute(entryId);
        LedgerEntryResponse response = ledgerEntryMapper.toResponse(compensatingEntry);
        idempotencyStore.put(idempotencyKey, ledgerEntryMapper.toJson(response), IDEMPOTENCY_TTL_SECONDS);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
