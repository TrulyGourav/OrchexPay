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
public class WalletResponse {

    private UUID id;
    private UUID merchantId;
    private String walletType;   // MAIN, ESCROW, VENDOR
    private UUID vendorUserId;  // non-null only when walletType is VENDOR
    private String currencyCode;
    private String status;
    private BigDecimal balance;  // derived from ledger, not stored
    private Instant createdAt;
    private Instant updatedAt;
}
