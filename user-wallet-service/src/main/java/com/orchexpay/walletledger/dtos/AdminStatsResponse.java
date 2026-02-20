package com.orchexpay.walletledger.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalMerchants;
    private long totalVendors;
    private long totalWallets;
    private long frozenWallets;
    private long totalLedgerEntries;
}
