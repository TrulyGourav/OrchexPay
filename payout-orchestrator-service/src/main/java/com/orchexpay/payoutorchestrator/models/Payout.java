package com.orchexpay.payoutorchestrator.models;

import com.orchexpay.payoutorchestrator.enums.PayoutStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payout aggregate. Owned by payout-orchestrator; state machine: CREATED → PROCESSING → SETTLED | FAILED.
 * ledgerEntryId = wallet-service ledger entry (PENDING debit) after reserve; used for confirm/reverse.
 */
@Entity
@Table(name = "payouts", indexes = {})
public class Payout {

    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "vendor_wallet_id", nullable = false)
    private UUID vendorWalletId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayoutStatus status;

    @Column(name = "ledger_entry_id")
    private UUID ledgerEntryId;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Payout() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }
    public UUID getVendorId() { return vendorId; }
    public void setVendorId(UUID vendorId) { this.vendorId = vendorId; }
    public UUID getVendorWalletId() { return vendorWalletId; }
    public void setVendorWalletId(UUID vendorWalletId) { this.vendorWalletId = vendorWalletId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public PayoutStatus getStatus() { return status; }
    public void setStatus(PayoutStatus status) { this.status = status; }
    public UUID getLedgerEntryId() { return ledgerEntryId; }
    public void setLedgerEntryId(UUID ledgerEntryId) { this.ledgerEntryId = ledgerEntryId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static PayoutBuilder builder() {
        return new PayoutBuilder();
    }

    public static final class PayoutBuilder {
        private UUID id;
        private UUID merchantId;
        private UUID vendorId;
        private UUID vendorWalletId;
        private BigDecimal amount;
        private String currencyCode;
        private PayoutStatus status;
        private UUID ledgerEntryId;
        private String idempotencyKey;
        private Instant createdAt;
        private Instant updatedAt;

        public PayoutBuilder id(UUID id) { this.id = id; return this; }
        public PayoutBuilder merchantId(UUID merchantId) { this.merchantId = merchantId; return this; }
        public PayoutBuilder vendorId(UUID vendorId) { this.vendorId = vendorId; return this; }
        public PayoutBuilder vendorWalletId(UUID vendorWalletId) { this.vendorWalletId = vendorWalletId; return this; }
        public PayoutBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public PayoutBuilder currencyCode(String currencyCode) { this.currencyCode = currencyCode; return this; }
        public PayoutBuilder status(PayoutStatus status) { this.status = status; return this; }
        public PayoutBuilder ledgerEntryId(UUID ledgerEntryId) { this.ledgerEntryId = ledgerEntryId; return this; }
        public PayoutBuilder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public PayoutBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public PayoutBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public Payout build() {
            Payout p = new Payout();
            p.setId(id);
            p.setMerchantId(merchantId);
            p.setVendorId(vendorId);
            p.setVendorWalletId(vendorWalletId);
            p.setAmount(amount);
            p.setCurrencyCode(currencyCode);
            p.setStatus(status);
            p.setLedgerEntryId(ledgerEntryId);
            p.setIdempotencyKey(idempotencyKey);
            p.setCreatedAt(createdAt);
            p.setUpdatedAt(updatedAt);
            return p;
        }
    }
}
