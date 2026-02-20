package com.orchexpay.walletledger.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotNull(message = "From wallet ID is required")
    private UUID fromWalletId;

    @NotBlank(message = "Reference ID is required for idempotency (e.g. order-001-split)")
    private String referenceId;

    @NotBlank(message = "Currency code is required")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be 3-letter ISO 4217 code")
    @Size(min = 3, max = 3)
    private String currencyCode;

    @NotNull(message = "Total debit amount is required")
    @DecimalMin(value = "0.0001", message = "Total amount must be positive")
    private BigDecimal totalAmount;

    @Valid
    @NotEmpty(message = "At least one credit leg is required")
    private List<CreditLegDto> creditLegs;

    private String description;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditLegDto {
        @NotNull
        private UUID toWalletId;
        @NotNull
        @DecimalMin(value = "0.0001")
        private BigDecimal amount;
    }
}
