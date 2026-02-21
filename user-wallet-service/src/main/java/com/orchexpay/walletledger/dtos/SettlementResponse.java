package com.orchexpay.walletledger.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponse {

    private UUID merchantId;
    private String currencyCode;
    private UUID escrowWalletId;
    private BigDecimal totalConfirmedEscrowCredits;
    private BigDecimal totalPayoutDebits;
    private BigDecimal totalRefundDebits;
    private BigDecimal expectedBalance;
    private BigDecimal ledgerNetBalance;
    private boolean reconciled;
}
