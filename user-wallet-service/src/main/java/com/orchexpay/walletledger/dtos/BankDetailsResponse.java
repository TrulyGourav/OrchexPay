package com.orchexpay.walletledger.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankDetailsResponse {
    private UUID userId;
    private String accountNumber;
    private String ifscCode;
    private String beneficiaryName;
    private Instant updatedAt;
}
