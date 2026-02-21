package com.orchexpay.walletledger.repositories;

import com.orchexpay.walletledger.models.Wallet;
import com.orchexpay.walletledger.enums.WalletStatus;
import com.orchexpay.walletledger.enums.WalletType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaWalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByMerchantIdAndCurrencyCode(UUID merchantId, String currencyCode);

    boolean existsByMerchantIdAndCurrencyCode(UUID merchantId, String currencyCode);

    Optional<Wallet> findByMerchantIdAndCurrencyCodeAndWalletTypeAndVendorUserId(
            UUID merchantId, String currencyCode, WalletType walletType, UUID vendorUserId);

    boolean existsByMerchantIdAndCurrencyCodeAndWalletTypeAndVendorUserId(
            UUID merchantId, String currencyCode, WalletType walletType, UUID vendorUserId);

    long countByStatus(WalletStatus status);
}
