package com.orchexpay.payoutorchestrator.controllers;

import com.orchexpay.payoutorchestrator.enums.CommissionType;
import com.orchexpay.payoutorchestrator.models.MerchantCommission;
import com.orchexpay.payoutorchestrator.repositories.MerchantCommissionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Manage per-merchant commission. Scalable: add new CommissionType without changing API contract.
 */
@RestController
@RequestMapping("/api/v1/merchants/{merchantId}/commission")
public class CommissionController {

    private final MerchantCommissionRepository commissionRepository;

    public CommissionController(MerchantCommissionRepository commissionRepository) {
        this.commissionRepository = commissionRepository;
    }

    @GetMapping
    public ResponseEntity<CommissionResponse> get(@PathVariable UUID merchantId) {
        return commissionRepository.findByMerchantId(merchantId)
                .map(CommissionController::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create or update commission for merchant. Idempotent put.
     */
    @PutMapping
    public ResponseEntity<CommissionResponse> put(@PathVariable UUID merchantId, @Valid @RequestBody CommissionRequest request) {
        Instant now = Instant.now();
        MerchantCommission config = commissionRepository.findByMerchantId(merchantId).orElseGet(() -> {
            MerchantCommission c = new MerchantCommission();
            c.setId(UUID.randomUUID());
            c.setMerchantId(merchantId);
            c.setCreatedAt(now);
            return c;
        });
        config.setCommissionType(CommissionType.valueOf(request.getCommissionType().toUpperCase()));
        config.setPercentageValue(request.getPercentageValue());
        config.setFixedAmount(request.getFixedAmount());
        config.setCurrencyCode(request.getCurrencyCode());
        config.setUpdatedAt(now);
        config = commissionRepository.save(config);
        return ResponseEntity.status(HttpStatus.OK).body(toResponse(config));
    }

    private static CommissionResponse toResponse(MerchantCommission c) {
        CommissionResponse r = new CommissionResponse();
        r.setMerchantId(c.getMerchantId());
        r.setCommissionType(c.getCommissionType().name());
        r.setPercentageValue(c.getPercentageValue());
        r.setFixedAmount(c.getFixedAmount());
        r.setCurrencyCode(c.getCurrencyCode());
        return r;
    }

    public static class CommissionRequest {
        @NotNull
        @Pattern(regexp = "PERCENTAGE|FIXED_PLUS_PERCENTAGE")
        private String commissionType;
        @NotNull
        @DecimalMin("0")
        @DecimalMax("100")
        private BigDecimal percentageValue;
        private BigDecimal fixedAmount;
        @Size(min = 3, max = 3)
        private String currencyCode;

        public String getCommissionType() { return commissionType; }
        public void setCommissionType(String commissionType) { this.commissionType = commissionType; }
        public BigDecimal getPercentageValue() { return percentageValue; }
        public void setPercentageValue(BigDecimal percentageValue) { this.percentageValue = percentageValue; }
        public BigDecimal getFixedAmount() { return fixedAmount; }
        public void setFixedAmount(BigDecimal fixedAmount) { this.fixedAmount = fixedAmount; }
        public String getCurrencyCode() { return currencyCode; }
        public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    }

    public static class CommissionResponse {
        private UUID merchantId;
        private String commissionType;
        private BigDecimal percentageValue;
        private BigDecimal fixedAmount;
        private String currencyCode;

        public UUID getMerchantId() { return merchantId; }
        public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }
        public String getCommissionType() { return commissionType; }
        public void setCommissionType(String commissionType) { this.commissionType = commissionType; }
        public BigDecimal getPercentageValue() { return percentageValue; }
        public void setPercentageValue(BigDecimal percentageValue) { this.percentageValue = percentageValue; }
        public BigDecimal getFixedAmount() { return fixedAmount; }
        public void setFixedAmount(BigDecimal fixedAmount) { this.fixedAmount = fixedAmount; }
        public String getCurrencyCode() { return currencyCode; }
        public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    }
}
