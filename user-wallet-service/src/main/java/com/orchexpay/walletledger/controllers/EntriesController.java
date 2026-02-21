package com.orchexpay.walletledger.controllers;

import com.orchexpay.walletledger.dtos.LedgerEntryResponse;
import com.orchexpay.walletledger.mappers.LedgerEntryMapper;
import com.orchexpay.walletledger.services.EntriesFilter;
import com.orchexpay.walletledger.services.GetLedgerEntriesUseCase;
import com.orchexpay.walletledger.services.GetUserByUsernameUseCase;
import com.orchexpay.walletledger.services.GetWalletUseCase;
import com.orchexpay.walletledger.models.LedgerEntry;
import com.orchexpay.walletledger.enums.Role;
import com.orchexpay.walletledger.models.User;
import com.orchexpay.walletledger.enums.EntryStatus;
import com.orchexpay.walletledger.enums.ReferenceType;
import com.orchexpay.walletledger.security.LedgerPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Paginated, filterable ledger entries. Supports wallet-scoped or merchant-scoped queries.
 * GET /api/v1/entries?walletId=... or ?merchantId=... with optional filters.
 */
@RestController
@RequestMapping("/api/v1/entries")
@RequiredArgsConstructor
public class EntriesController {

    private final GetLedgerEntriesUseCase getLedgerEntriesUseCase;
    private final GetWalletUseCase getWalletUseCase;
    private final GetUserByUsernameUseCase getUserByUsernameUseCase;
    private final LedgerEntryMapper ledgerEntryMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MERCHANT', 'VENDOR')")
    public ResponseEntity<Page<LedgerEntryResponse>> getEntries(
            @RequestParam(required = false) UUID walletId,
            @RequestParam(required = false) UUID merchantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String referenceType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            Authentication authentication) {
        if (walletId == null && merchantId == null) {
            return ResponseEntity.badRequest().build();
        }
        User currentUser = getUserByUsernameUseCase.execute(((LedgerPrincipal) authentication.getPrincipal()).username());
        boolean isAdmin = currentUser.hasRole(Role.ADMIN);

        if (walletId != null && !isAdmin) {
            var walletWithBalance = getWalletUseCase.execute(walletId);
            if (currentUser.hasRole(Role.VENDOR) && (walletWithBalance.wallet().getVendorUserId() == null || !walletWithBalance.wallet().getVendorUserId().equals(currentUser.getId()))) {
                throw new AccessDeniedException("Vendor can only view own wallet entries");
            }
            if (currentUser.hasRole(Role.MERCHANT) && (currentUser.getMerchantId() == null || !currentUser.getMerchantId().equals(walletWithBalance.wallet().getMerchantId()))) {
                throw new AccessDeniedException("Merchant can only view own merchant entries");
            }
        }
        if (merchantId != null && !isAdmin && (currentUser.getMerchantId() == null || !currentUser.getMerchantId().equals(merchantId))) {
            throw new AccessDeniedException("Merchant can only view own merchant entries");
        }

        ReferenceType refType = parseReferenceType(referenceType);
        EntryStatus entryStatus = parseEntryStatus(status);
        EntriesFilter filter = EntriesFilter.builder()
                .walletId(walletId)
                .merchantId(merchantId)
                .from(from)
                .to(to)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .referenceType(refType)
                .status(entryStatus)
                .build();

        Sort order = parseSort(sort);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), order);
        Page<LedgerEntry> entries = getLedgerEntriesUseCase.execute(filter, pageable);
        return ResponseEntity.ok(entries.map(ledgerEntryMapper::toResponse));
    }

    private static ReferenceType parseReferenceType(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return ReferenceType.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static EntryStatus parseEntryStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return EntryStatus.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        String prop = parts.length > 0 ? parts[0].trim() : "createdAt";
        boolean desc = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim());
        return desc ? Sort.by(prop).descending() : Sort.by(prop).ascending();
    }
}
