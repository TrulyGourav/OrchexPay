package com.orchexpay.payoutorchestrator.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Tracks orders that had payment success (escrow credited) but not yet completed (split).
 * Used to show "pending completion" orders per vendor in the merchant UI.
 */
@Entity
@Table(name = "pending_orders", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "merchant_id", "order_id" })
})
public class PendingOrder {

    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "order_id", nullable = false, length = 255)
    private String orderId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "split_done", nullable = false)
    private boolean splitDone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public PendingOrder() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }
    public UUID getVendorId() { return vendorId; }
    public void setVendorId(UUID vendorId) { this.vendorId = vendorId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public boolean isSplitDone() { return splitDone; }
    public void setSplitDone(boolean splitDone) { this.splitDone = splitDone; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
