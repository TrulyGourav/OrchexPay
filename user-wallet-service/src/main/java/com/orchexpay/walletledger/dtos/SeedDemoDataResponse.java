package com.orchexpay.walletledger.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeedDemoDataResponse {

    private int merchantsCreated;
    private int vendorsCreated;
}
