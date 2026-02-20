package com.orchexpay.walletledger.models;

import com.orchexpay.walletledger.enums.WalletStatus;
import com.orchexpay.walletledger.enums.WalletType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Wallet aggregate root and JPA entity.
 * Balance is NEVER stored; it is always derived from ledger entries (see LedgerEntry).
 */
@Entity
@Table(name = "wallets", uniqueConstraints = {
        @UniqueConstraint(name = "uk_wallets_merchant_currency_type_vendor",
                columnNames = {"merchant_id", "currency_code", "wallet_type", "vendor_user_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type", length = 20)
    private WalletType walletType;  // null treated as MAIN for backward compatibility

    @Column(name = "vendor_user_id")
    private UUID vendorUserId;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WalletStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Wallet type; null (legacy) is treated as MAIN. */
    public WalletType getWalletType() {
        return walletType != null ? walletType : WalletType.MAIN;
    }

    /** Domain accessor: currency as value object (derived from persisted currencyCode). */
    public Currency getCurrency() {
        return Currency.of(currencyCode);
    }

    /**
     * Returns the current balance for this wallet. Caller must supply the computed balance
     * from ledger entries (domain service or application layer).
     */
    public Money balance(Money computedBalance) {
        if (!getCurrency().equals(computedBalance.getCurrency())) {
            throw new IllegalArgumentException("Computed balance currency must match wallet currency");
        }
        return computedBalance;
    }

    public boolean isActive() {
        return status == WalletStatus.ACTIVE;
    }
}
