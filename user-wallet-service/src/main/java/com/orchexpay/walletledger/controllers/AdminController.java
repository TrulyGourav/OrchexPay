package com.orchexpay.walletledger.controllers;

import com.orchexpay.walletledger.dtos.AdminStatsResponse;
import com.orchexpay.walletledger.dtos.SeedDemoDataResponse;
import com.orchexpay.walletledger.services.GetAdminStatsUseCase;
import com.orchexpay.walletledger.services.SeedDemoDataUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final GetAdminStatsUseCase getAdminStatsUseCase;
    private final SeedDemoDataUseCase seedDemoDataUseCase;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminStatsResponse> getStats() {
        var result = getAdminStatsUseCase.execute();
        AdminStatsResponse response = AdminStatsResponse.builder()
                .totalMerchants(result.totalMerchants())
                .totalVendors(result.totalVendors())
                .totalWallets(result.totalWallets())
                .frozenWallets(result.frozenWallets())
                .totalLedgerEntries(result.totalLedgerEntries())
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Seeds demo data: 11 merchants and 6-8 vendors per merchant (password 'password', currency INR).
     * Idempotent: skips usernames that already exist. Admin only.
     */
    @PostMapping("/seed-demo-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeedDemoDataResponse> seedDemoData() {
        var result = seedDemoDataUseCase.execute();
        return ResponseEntity.ok(SeedDemoDataResponse.builder()
                .merchantsCreated(result.merchantsCreated())
                .vendorsCreated(result.vendorsCreated())
                .build());
    }
}
