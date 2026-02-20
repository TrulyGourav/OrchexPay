package com.orchexpay.walletledger.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BankDetailsRequest {
    @NotBlank(message = "Account number is required")
    @Size(max = 50)
    private String accountNumber;

    @Size(max = 20)
    private String ifscCode;

    @NotBlank(message = "Beneficiary name is required")
    @Size(max = 255)
    private String beneficiaryName;
}
