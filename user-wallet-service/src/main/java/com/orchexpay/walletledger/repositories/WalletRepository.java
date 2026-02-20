package com.orchexpay.walletledger.repositories;

import com.orchexpay.walletledger.models.Wallet;
import com.orchexpay.walletledger.enums.WalletStatus;
import com.orchexpay.walletledger.enums.WalletType;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for wallet persistence. Implemented in infrastructure.
 */
public interface WalletRepository {

    Wallet save(Wallet wallet);

    Optional<Wallet> findById(UUID id);

    Optional<Wallet> findByMerchantIdAndCurrency(UUID merchantId, String currencyCode);

    boolean existsByMerchantIdAndCurrency(UUID merchantId, String currencyCode);

    Optional<Wallet> findByMerchantIdAndCurrencyCodeAndWalletTypeAndVendorUserId(
            UUID merchantId, String currencyCode, WalletType walletType, UUID vendorUserId);

    boolean existsByMerchantIdAndCurrencyCodeAndWalletTypeAndVendorUserId(
            UUID merchantId, String currencyCode, WalletType walletType, UUID vendorUserId);

    long count();

    long countByStatus(WalletStatus status);
}
