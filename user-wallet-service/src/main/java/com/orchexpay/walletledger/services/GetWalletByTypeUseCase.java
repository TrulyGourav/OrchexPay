package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.exceptions.WalletNotFoundException;
import com.orchexpay.walletledger.repositories.WalletRepository;
import com.orchexpay.walletledger.models.Wallet;
import com.orchexpay.walletledger.enums.WalletType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Resolves wallet by merchant, currency, and type (and optional vendor for VENDOR wallets).
 * Used by payout-orchestrator to resolve escrow/main/vendor wallet IDs for webhooks.
 */
@Service
@RequiredArgsConstructor
public class GetWalletByTypeUseCase {

    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public Wallet execute(UUID merchantId, String currencyCode, WalletType walletType, UUID vendorUserId) {
        return walletRepository
                .findByMerchantIdAndCurrencyCodeAndWalletTypeAndVendorUserId(merchantId, currencyCode, walletType, vendorUserId)
                .orElseThrow(() -> new WalletNotFoundException("No wallet for merchant " + merchantId + " currency " + currencyCode + " type " + walletType + (vendorUserId != null ? " vendor " + vendorUserId : "")));
    }
}
