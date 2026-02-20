package com.orchexpay.walletledger.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoneyMovementRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0001", message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Currency code is required")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be 3-letter ISO 4217 code")
    @Size(min = 3, max = 3)
    private String currencyCode;

    @NotBlank(message = "Reference ID is required for idempotency")
    private String referenceId;

    /** ORDER | PAYOUT | REFUND | REVERSAL. Default ORDER for credit/debit. */
    private String referenceType;

    private String description;
}
