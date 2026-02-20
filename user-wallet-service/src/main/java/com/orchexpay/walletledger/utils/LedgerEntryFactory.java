package com.orchexpay.walletledger.utils;

import com.orchexpay.walletledger.models.LedgerEntry;
import com.orchexpay.walletledger.models.Money;
import com.orchexpay.walletledger.enums.EntryStatus;
import com.orchexpay.walletledger.enums.EntryType;
import com.orchexpay.walletledger.enums.ReferenceType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Factory for creating ledger entries. All entries require merchantId, referenceType, and status
 * for audit and balance derivation (balance = CONFIRMED credits − CONFIRMED debits).
 */
@Component
public class LedgerEntryFactory {

    /**
     * Creates a CREDIT entry (increases wallet balance when CONFIRMED).
     */
    public LedgerEntry createCredit(UUID walletId, UUID merchantId, UUID vendorId,
                                    Money amount, ReferenceType referenceType, String referenceId,
                                    EntryStatus status, String description) {
        return LedgerEntry.builder()
                .id(UUID.randomUUID())
                .walletId(walletId)
                .merchantId(merchantId)
                .vendorId(vendorId)
                .type(EntryType.CREDIT)
                .amountValue(amount.getAmount())
                .currencyCode(amount.getCurrency().getCode())
                .referenceType(referenceType != null ? referenceType : ReferenceType.ORDER)
                .referenceId(referenceId != null ? referenceId : "")
                .status(status != null ? status : EntryStatus.CONFIRMED)
                .description(description != null ? description : "")
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Creates a DEBIT entry (decreases wallet balance when CONFIRMED).
     */
    public LedgerEntry createDebit(UUID walletId, UUID merchantId, UUID vendorId,
                                   Money amount, ReferenceType referenceType, String referenceId,
                                   EntryStatus status, String description) {
        return LedgerEntry.builder()
                .id(UUID.randomUUID())
                .walletId(walletId)
                .merchantId(merchantId)
                .vendorId(vendorId)
                .type(EntryType.DEBIT)
                .amountValue(amount.getAmount())
                .currencyCode(amount.getCurrency().getCode())
                .referenceType(referenceType != null ? referenceType : ReferenceType.ORDER)
                .referenceId(referenceId != null ? referenceId : "")
                .status(status != null ? status : EntryStatus.CONFIRMED)
                .description(description != null ? description : "")
                .createdAt(Instant.now())
                .build();
    }
}
