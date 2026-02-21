package com.orchexpay.walletledger.repositories;

import com.orchexpay.walletledger.models.Wallet;
import com.orchexpay.walletledger.enums.WalletStatus;
import com.orchexpay.walletledger.enums.WalletType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WalletRepositoryAdapter implements WalletRepository {

    private final JpaWalletRepository jpaWalletRepository;

    @Override
    public Wallet save(Wallet wallet) {
        if (wallet.getId() == null) wallet.setId(UUID.randomUUID());
        return jpaWalletRepository.save(wallet);
    }

    @Override
    public Optional<Wallet> findById(UUID id) {
        return jpaWalletRepository.findById(id);
    }

    @Override
    public Optional<Wallet> findByMerchantIdAndCurrency(UUID merchantId, String currencyCode) {
        return jpaWalletRepository.findByMerchantIdAndCurrencyCode(merchantId, currencyCode);
    }

    @Override
    public boolean existsByMerchantIdAndCurrency(UUID merchantId, String currencyCode) {
        return jpaWalletRepository.existsByMerchantIdAndCurrencyCode(merchantId, currencyCode);
    }

    @Override
    public Optional<Wallet> findByMerchantIdAndCurrencyCodeAndWalletTypeAndVendorUserId(
            UUID merchantId, String currencyCode, WalletType walletType, UUID vendorUserId) {
        return jpaWalletRepository.findByMerchantIdAndCurrencyCodeAndWalletTypeAndVendorUserId(
                merchantId, currencyCode, walletType, vendorUserId);
    }

    @Override
    public boolean existsByMerchantIdAndCurrencyCodeAndWalletTypeAndVendorUserId(
            UUID merchantId, String currencyCode, WalletType walletType, UUID vendorUserId) {
        return jpaWalletRepository.existsByMerchantIdAndCurrencyCodeAndWalletTypeAndVendorUserId(
                merchantId, currencyCode, walletType, vendorUserId);
    }

    @Override
    public long count() {
        return jpaWalletRepository.count();
    }

    @Override
    public long countByStatus(WalletStatus status) {
        return jpaWalletRepository.countByStatus(status);
    }
}
