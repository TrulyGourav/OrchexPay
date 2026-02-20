package com.orchexpay.walletledger.mappers;

import com.orchexpay.walletledger.dtos.WalletResponse;
import com.orchexpay.walletledger.services.GetWalletUseCase.WalletWithBalance;
import com.orchexpay.walletledger.models.Wallet;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {

    public WalletResponse toResponse(Wallet wallet, java.math.BigDecimal balance) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .merchantId(wallet.getMerchantId())
                .walletType(wallet.getWalletType().name())
                .vendorUserId(wallet.getVendorUserId())
                .currencyCode(wallet.getCurrency().getCode())
                .status(wallet.getStatus().name())
                .balance(balance)
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    public WalletResponse toResponse(WalletWithBalance walletWithBalance) {
        return toResponse(
                walletWithBalance.wallet(),
                walletWithBalance.balance().getAmount()
        );
    }

    public WalletResponse toResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .merchantId(wallet.getMerchantId())
                .walletType(wallet.getWalletType().name())
                .vendorUserId(wallet.getVendorUserId())
                .currencyCode(wallet.getCurrency().getCode())
                .status(wallet.getStatus().name())
                .balance(null)  // callers must use GetWallet for balance
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}
