package com.orchexpay.walletledger.controllers;

import com.orchexpay.walletledger.dtos.AddVendorRequest;
import com.orchexpay.walletledger.dtos.SettlementResponse;
import com.orchexpay.walletledger.dtos.UserResponse;
import com.orchexpay.walletledger.dtos.VendorSummaryResponse;
import com.orchexpay.walletledger.dtos.WalletResponse;
import com.orchexpay.walletledger.mappers.UserMapper;
import com.orchexpay.walletledger.mappers.WalletMapper;
import com.orchexpay.walletledger.services.AddVendorResult;
import com.orchexpay.walletledger.services.AddVendorUseCase;
import com.orchexpay.walletledger.services.GetVendorsByMerchantUseCase;
import com.orchexpay.walletledger.services.GetWalletByTypeUseCase;
import com.orchexpay.walletledger.services.GetWalletUseCase;
import com.orchexpay.walletledger.services.SettlementCalculationUseCase;
import com.orchexpay.walletledger.repositories.UserRepository;
import com.orchexpay.walletledger.models.User;
import com.orchexpay.walletledger.enums.WalletType;
import com.orchexpay.walletledger.security.LedgerPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Merchant and vendor management. List merchants (ADMIN), list/add vendors (MERCHANT/ADMIN), wallets by type, settlement.
 */
@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantVendorController {

    private final AddVendorUseCase addVendorUseCase;
    private final GetWalletByTypeUseCase getWalletByTypeUseCase;
    private final GetWalletUseCase getWalletUseCase;
    private final GetVendorsByMerchantUseCase getVendorsByMerchantUseCase;
    private final SettlementCalculationUseCase settlementCalculationUseCase;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final WalletMapper walletMapper;

    /**
     * Resolve wallet by type for the merchant (and optional vendor). Used by payout-orchestrator for webhooks.
     * ADMIN/SYSTEM: any merchant. MERCHANT: only own merchantId.
     */
    @GetMapping("/{merchantId}/wallets/by-type")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM', 'MERCHANT')")
    public ResponseEntity<WalletResponse> getWalletByType(
            @PathVariable UUID merchantId,
            @RequestParam String currencyCode,
            @RequestParam String walletType,
            @RequestParam(required = false) UUID vendorUserId,
            Authentication authentication) {
        if (authentication.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SYSTEM".equals(a.getAuthority()))) {
            LedgerPrincipal principal = (LedgerPrincipal) authentication.getPrincipal();
            if (!principal.hasMerchantId(merchantId)) {
                throw new AccessDeniedException("Merchant can only resolve wallets for own account");
            }
        }
        WalletType type = WalletType.valueOf(walletType.toUpperCase());
        var walletWithBalance = getWalletUseCase.execute(
                getWalletByTypeUseCase.execute(merchantId, currencyCode, type, vendorUserId).getId());
        return ResponseEntity.ok(walletMapper.toResponse(walletWithBalance));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> listMerchants(Pageable pageable) {
        Page<User> page = userRepository.findMerchantUsers(pageable);
        return ResponseEntity.ok(page.map(u -> userMapper.toResponse(u)));
    }

    @GetMapping("/{merchantId}/vendors")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    public ResponseEntity<List<VendorSummaryResponse>> listVendors(
            @PathVariable UUID merchantId,
            Authentication authentication) {
        if (authentication.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            LedgerPrincipal principal = (LedgerPrincipal) authentication.getPrincipal();
            if (!principal.hasMerchantId(merchantId)) {
                throw new AccessDeniedException("Merchant can only list own vendors");
            }
        }
        List<VendorSummaryResponse> list = getVendorsByMerchantUseCase.execute(merchantId).stream()
                .map(v -> VendorSummaryResponse.builder()
                        .userId(v.userId())
                        .username(v.username())
                        .vendorWalletId(v.vendorWalletId())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{merchantId}/vendors")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    public ResponseEntity<UserResponse> addVendor(
            @PathVariable UUID merchantId,
            @Valid @RequestBody AddVendorRequest request,
            Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin) {
            LedgerPrincipal principal = (LedgerPrincipal) authentication.getPrincipal();
            if (!principal.hasMerchantId(merchantId)) {
                throw new AccessDeniedException("Merchant can only add vendors to their own account");
            }
        }
        AddVendorResult result = addVendorUseCase.execute(
                merchantId,
                request.getUsername(),
                request.getPassword(),
                request.getCurrencyCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                userMapper.toResponse(result.user(), null, null, result.vendorWalletId()));
    }

    /**
     * Daily settlement calculation for merchant ESCROW. Compares expected balance (credits - payouts - refunds) with ledger net.
     * If !reconciled, flag for investigation.
     */
    @GetMapping("/{merchantId}/settlement")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    public ResponseEntity<SettlementResponse> getSettlement(
            @PathVariable UUID merchantId,
            @RequestParam(defaultValue = "INR") String currencyCode,
            Authentication authentication) {
        if (authentication.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            LedgerPrincipal principal = (LedgerPrincipal) authentication.getPrincipal();
            if (!principal.hasMerchantId(merchantId)) {
                throw new AccessDeniedException("Merchant can only view own settlement");
            }
        }
        var result = settlementCalculationUseCase.execute(merchantId, currencyCode);
        SettlementResponse response = SettlementResponse.builder()
                .merchantId(result.merchantId())
                .currencyCode(result.currencyCode())
                .escrowWalletId(result.escrowWalletId())
                .totalConfirmedEscrowCredits(result.totalConfirmedEscrowCredits())
                .totalPayoutDebits(result.totalPayoutDebits())
                .totalRefundDebits(result.totalRefundDebits())
                .expectedBalance(result.expectedBalance())
                .ledgerNetBalance(result.ledgerNetBalance())
                .reconciled(result.reconciled())
                .build();
        return ResponseEntity.ok(response);
    }
}
