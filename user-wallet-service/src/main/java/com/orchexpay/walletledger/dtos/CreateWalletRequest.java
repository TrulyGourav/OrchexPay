package com.orchexpay.walletledger.dtos;

import com.orchexpay.walletledger.enums.WalletType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWalletRequest {

    @NotBlank(message = "Currency code is required")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be 3-letter ISO 4217 code")
    @Size(min = 3, max = 3)
    private String currencyCode;

    /** Wallet type: MAIN, ESCROW, or VENDOR. Default MAIN if omitted. */
    private WalletType walletType;

    /** Required when walletType is VENDOR: the vendor user id (must belong to this merchant). */
    private java.util.UUID vendorUserId;
}
