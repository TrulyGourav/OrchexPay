package com.orchexpay.walletledger.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResultResponse {

    private LedgerEntryResponse debitEntry;
    private List<LedgerEntryResponse> creditEntries;
    private boolean idempotent;

    public static TransferResultResponse idempotent() {
        return TransferResultResponse.builder()
                .creditEntries(Collections.emptyList())
                .idempotent(true)
                .build();
    }
}
