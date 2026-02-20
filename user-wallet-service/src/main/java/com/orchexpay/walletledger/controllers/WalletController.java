package com.orchexpay.walletledger.controllers;

import com.orchexpay.walletledger.dtos.LedgerEntryResponse;
import com.orchexpay.walletledger.dtos.MoneyMovementRequest;
import com.orchexpay.walletledger.dtos.TransferRequest;
import com.orchexpay.walletledger.dtos.TransferResultResponse;
import com.orchexpay.walletledger.dtos.WalletResponse;
import com.orchexpay.walletledger.mappers.LedgerEntryMapper;
import com.orchexpay.walletledger.mappers.WalletMapper;
import com.orchexpay.walletledger.configs.IdempotencyStore;
import com.orchexpay.walletledger.services.ConfirmReservationUseCase;
import com.orchexpay.walletledger.services.CreditWalletUseCase;
import com.orchexpay.walletledger.services.GetUserByUsernameUseCase;
import com.orchexpay.walletledger.services.DebitWalletUseCase;
import com.orchexpay.walletledger.services.UpdateWalletStatusUseCase;
import com.orchexpay.walletledger.services.GetWalletUseCase;
import com.orchexpay.walletledger.services.ReserveWalletUseCase;
import com.orchexpay.walletledger.services.ReverseReservationUseCase;
import com.orchexpay.walletledger.services.TransferUseCase;
import com.orchexpay.walletledger.models.LedgerEntry;
import com.orchexpay.walletledger.models.Money;
import com.orchexpay.walletledger.enums.ReferenceType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import com.orchexpay.walletledger.enums.Role;
import com.orchexpay.walletledger.models.User;
import com.orchexpay.walletledger.enums.WalletStatus;
import com.orchexpay.walletledger.security.LedgerPrincipal;

/**
 * Accounting-only API: performs ledger operations (credit/debit) and wallet read.
 * Wallets are created only at account creation (MERCHANT: MAIN+ESCROW; VENDOR: single wallet). No ad-hoc wallet creation.
 * This service must NEVER: call bank APIs, manage payout state machine, or process webhooks.
 * See docs/ARCHITECTURE_AUDIT.md for service boundaries.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WalletController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final long IDEMPOTENCY_TTL_SECONDS = 86400; // 24 hours

    private final GetWalletUseCase getWalletUseCase;
    private final GetUserByUsernameUseCase getUserByUsernameUseCase;
    private final CreditWalletUseCase creditWalletUseCase;
    private final DebitWalletUseCase debitWalletUseCase;
    private final ReserveWalletUseCase reserveWalletUseCase;
    private final ConfirmReservationUseCase confirmReservationUseCase;
    private final ReverseReservationUseCase reverseReservationUseCase;
    private final TransferUseCase transferUseCase;
    private final UpdateWalletStatusUseCase updateWalletStatusUseCase;
    private final WalletMapper walletMapper;
    private final LedgerEntryMapper ledgerEntryMapper;
    private final IdempotencyStore idempotencyStore;

    @PostMapping("/wallets/{walletId}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletResponse> freezeWallet(@PathVariable UUID walletId) {
        updateWalletStatusUseCase.execute(walletId, WalletStatus.FROZEN);
        return ResponseEntity.ok(walletMapper.toResponse(getWalletUseCase.execute(walletId)));
    }

    @PostMapping("/wallets/{walletId}/unfreeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletResponse> unfreezeWallet(@PathVariable UUID walletId) {
        updateWalletStatusUseCase.execute(walletId, WalletStatus.ACTIVE);
        return ResponseEntity.ok(walletMapper.toResponse(getWalletUseCase.execute(walletId)));
    }

    @GetMapping("/wallets/{walletId}")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN', 'VENDOR')")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable UUID walletId, Authentication authentication) {
        GetWalletUseCase.WalletWithBalance result = getWalletUseCase.execute(walletId);
        if (authentication != null && authentication.getPrincipal() instanceof LedgerPrincipal principal) {
            User currentUser = getUserByUsernameUseCase.execute(principal.username());
            if (currentUser.hasRole(Role.VENDOR) && (result.wallet().getVendorUserId() == null || !result.wallet().getVendorUserId().equals(currentUser.getId()))) {
                throw new org.springframework.security.access.AccessDeniedException("Vendor can only access their own wallet");
            }
        }
        return ResponseEntity.ok(walletMapper.toResponse(result));
    }

    @PostMapping("/wallets/{walletId}/credit")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN', 'SYSTEM')")
    public ResponseEntity<LedgerEntryResponse> creditWallet(
            @PathVariable UUID walletId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = true) String idempotencyKey,
            @Valid @RequestBody MoneyMovementRequest request) {
        Optional<String> cached = idempotencyStore.getIfPresent(idempotencyKey);
        if (cached.isPresent()) {
            return ResponseEntity.ok().body(ledgerEntryMapper.toResponseFromJson(cached.get()));
        }
        Money amount = Money.of(request.getAmount(), request.getCurrencyCode());
        ReferenceType refType = request.getReferenceType() != null ? ReferenceType.valueOf(request.getReferenceType().toUpperCase()) : ReferenceType.ORDER;
        LedgerEntry entry = creditWalletUseCase.execute(walletId, amount, refType, request.getReferenceId(), request.getDescription());
        LedgerEntryResponse response = ledgerEntryMapper.toResponse(entry);
        idempotencyStore.put(idempotencyKey, ledgerEntryMapper.toJson(response), IDEMPOTENCY_TTL_SECONDS);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/wallets/{walletId}/debit")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN', 'SYSTEM')")
    public ResponseEntity<LedgerEntryResponse> debitWallet(
            @PathVariable UUID walletId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = true) String idempotencyKey,
            @Valid @RequestBody MoneyMovementRequest request) {
        Optional<String> cached = idempotencyStore.getIfPresent(idempotencyKey);
        if (cached.isPresent()) {
            return ResponseEntity.ok().body(ledgerEntryMapper.toResponseFromJson(cached.get()));
        }
        Money amount = Money.of(request.getAmount(), request.getCurrencyCode());
        ReferenceType refType = request.getReferenceType() != null ? ReferenceType.valueOf(request.getReferenceType().toUpperCase()) : ReferenceType.ORDER;
        LedgerEntry entry = debitWalletUseCase.execute(walletId, amount, refType, request.getReferenceId(), request.getDescription());
        LedgerEntryResponse response = ledgerEntryMapper.toResponse(entry);
        idempotencyStore.put(idempotencyKey, ledgerEntryMapper.toJson(response), IDEMPOTENCY_TTL_SECONDS);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Reserve funds (PENDING DEBIT, reference_type=PAYOUT). Used by payout-orchestrator before bank call.
     * On bank success call confirm; on failure call reverse.
     * VENDOR may only reserve their own wallet (vendor wallet linked to their user id).
     */
    @PostMapping("/wallets/{walletId}/reserve")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN', 'SYSTEM', 'VENDOR')")
    public ResponseEntity<LedgerEntryResponse> reserveWallet(
            @PathVariable UUID walletId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = true) String idempotencyKey,
            @Valid @RequestBody MoneyMovementRequest request,
            Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof LedgerPrincipal principal) {
            User currentUser = getUserByUsernameUseCase.execute(principal.username());
            if (currentUser.hasRole(Role.VENDOR)) {
                var walletWithBalance = getWalletUseCase.execute(walletId);
                if (walletWithBalance.wallet().getVendorUserId() == null || !walletWithBalance.wallet().getVendorUserId().equals(currentUser.getId())) {
                    throw new org.springframework.security.access.AccessDeniedException("Vendor can only reserve their own wallet");
                }
            }
        }
        Optional<String> cached = idempotencyStore.getIfPresent(idempotencyKey);
        if (cached.isPresent()) {
            return ResponseEntity.ok().body(ledgerEntryMapper.toResponseFromJson(cached.get()));
        }
        Money amount = Money.of(request.getAmount(), request.getCurrencyCode());
        LedgerEntry entry = reserveWalletUseCase.execute(walletId, amount, request.getReferenceId(), request.getDescription());
        LedgerEntryResponse response = ledgerEntryMapper.toResponse(entry);
        idempotencyStore.put(idempotencyKey, ledgerEntryMapper.toJson(response), IDEMPOTENCY_TTL_SECONDS);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Atomic transfer (e.g. ESCROW → VENDOR + MAIN). Idempotent by (fromWalletId, referenceId).
     */
    @PostMapping("/transfers")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN', 'SYSTEM')")
    public ResponseEntity<TransferResultResponse> transfer(
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = true) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        Optional<String> cached = idempotencyStore.getIfPresent(idempotencyKey);
        if (cached.isPresent()) {
            return ResponseEntity.ok().body(ledgerEntryMapper.transferResultFromJson(cached.get()));
        }
        var legs = request.getCreditLegs().stream()
                .map(l -> new TransferUseCase.CreditLeg(l.getToWalletId(), l.getAmount()))
                .toList();
        TransferUseCase.TransferResult result = transferUseCase.execute(
                request.getFromWalletId(), request.getReferenceId(), request.getCurrencyCode(),
                request.getTotalAmount(), legs, request.getDescription());
        TransferResultResponse response = toTransferResultResponse(result);
        idempotencyStore.put(idempotencyKey, ledgerEntryMapper.transferResultToJson(response), IDEMPOTENCY_TTL_SECONDS);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private TransferResultResponse toTransferResultResponse(TransferUseCase.TransferResult result) {
        if (result.reused()) {
            return TransferResultResponse.idempotent();
        }
        return TransferResultResponse.builder()
                .debitEntry(ledgerEntryMapper.toResponse(result.debitEntry()))
                .creditEntries(result.creditEntries().stream().map(ledgerEntryMapper::toResponse).toList())
                .idempotent(false)
                .build();
    }
}
