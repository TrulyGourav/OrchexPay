package com.orchexpay.walletledger.models;

import com.orchexpay.walletledger.enums.EntryStatus;
import com.orchexpay.walletledger.enums.EntryType;
import com.orchexpay.walletledger.enums.ReferenceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Ledger entry (double-entry bookkeeping) and JPA entity.
 * Balance is NEVER stored; it is derived as SUM(CONFIRMED credits) − SUM(CONFIRMED debits).
 * PENDING entries are reserves; REVERSED entries are excluded from balance.
 *
 * Immutability: amountValue, type, referenceType, referenceId, walletId, merchantId, vendorId
 * must never be updated after persist. The only allowed mutation is status transition for
 * reserve lifecycle: PENDING → CONFIRMED (confirm) or PENDING → REVERSED (reverse).
 */
@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_ledger_merchant_id", columnList = "merchant_id"),
        @Index(name = "idx_ledger_wallet_ref_type", columnList = "wallet_id, reference_id, reference_type", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry {

    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "vendor_id")
    private UUID vendorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private EntryType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountValue;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 20)
    private ReferenceType referenceType;

    @Column(name = "reference_id", nullable = false)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EntryStatus status;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Domain accessor: amount as value object (derived from persisted amountValue + currencyCode). */
    public Money getAmount() {
        return Money.of(amountValue != null ? amountValue : BigDecimal.ZERO, currencyCode);
    }

    /** True if this entry contributes to balance (only CONFIRMED entries do). */
    public boolean isConfirmed() {
        return status == EntryStatus.CONFIRMED;
    }

    /** True if this entry was reversed (compensating REVERSAL entry exists). */
    public boolean isReversed() {
        return status == EntryStatus.REVERSED;
    }
}
