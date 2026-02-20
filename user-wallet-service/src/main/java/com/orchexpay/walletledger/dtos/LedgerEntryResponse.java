package com.orchexpay.walletledger.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntryResponse {

    private UUID id;
    private UUID walletId;
    private String type;  // CREDIT | DEBIT
    private BigDecimal amount;
    private String currencyCode;
    private String referenceType;  // ORDER | PAYOUT | REFUND | REVERSAL
    private String referenceId;
    private String status;  // PENDING | CONFIRMED | REVERSED
    private String description;
    private Instant createdAt;
}
