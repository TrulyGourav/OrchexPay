package com.orchexpay.walletledger.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String username;
    private Set<String> roles;
    private UUID merchantId;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    /** Set for MERCHANT: MAIN wallet id (created at account creation). */
    private UUID mainWalletId;
    /** Set for MERCHANT: ESCROW wallet id (created at account creation). */
    private UUID escrowWalletId;
    /** Set for VENDOR: single VENDOR wallet id (created at account creation). */
    private UUID vendorWalletId;
}
