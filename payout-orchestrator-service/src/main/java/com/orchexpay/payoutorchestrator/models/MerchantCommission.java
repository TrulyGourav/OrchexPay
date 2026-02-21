package com.orchexpay.payoutorchestrator.models;

import com.orchexpay.payoutorchestrator.enums.CommissionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Per-merchant commission config. One active config per merchant; scalable for new CommissionType values.
 */
@Entity
@Table(name = "merchant_commissions", uniqueConstraints = @UniqueConstraint(columnNames = "merchant_id"))
public class MerchantCommission {

    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "merchant_id", nullable = false, unique = true)
    private UUID merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_type", nullable = false, length = 30)
    private CommissionType commissionType;

    /** e.g. 20.00 for 20% */
    @Column(name = "percentage_value", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentageValue;

    /** Optional; used when commission_type = FIXED_PLUS_PERCENTAGE */
    @Column(name = "fixed_amount", precision = 19, scale = 4)
    private BigDecimal fixedAmount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public MerchantCommission() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }
    public CommissionType getCommissionType() { return commissionType; }
    public void setCommissionType(CommissionType commissionType) { this.commissionType = commissionType; }
    public BigDecimal getPercentageValue() { return percentageValue; }
    public void setPercentageValue(BigDecimal percentageValue) { this.percentageValue = percentageValue; }
    public BigDecimal getFixedAmount() { return fixedAmount; }
    public void setFixedAmount(BigDecimal fixedAmount) { this.fixedAmount = fixedAmount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
