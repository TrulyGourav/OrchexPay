package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.enums.EntryStatus;
import com.orchexpay.walletledger.enums.ReferenceType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class EntriesFilter {
    UUID walletId;
    UUID merchantId;
    Instant from;
    Instant to;
    BigDecimal minAmount;
    BigDecimal maxAmount;
    ReferenceType referenceType;
    EntryStatus status;
}
