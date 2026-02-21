package com.orchexpay.walletledger.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddVendorRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 2, max = 255)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /** Currency for the vendor's single wallet. Defaults to INR if omitted. */
    @Size(min = 3, max = 3, message = "Currency must be 3-letter ISO 4217 code")
    private String currencyCode;
}
