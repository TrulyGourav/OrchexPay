package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.exceptions.WalletAlreadyExistsException;
import com.orchexpay.walletledger.events.DomainEventPublisher;
import com.orchexpay.walletledger.repositories.UserRepository;
import com.orchexpay.walletledger.repositories.WalletRepository;
import com.orchexpay.walletledger.events.WalletCreatedEvent;
import com.orchexpay.walletledger.enums.Role;
import com.orchexpay.walletledger.models.Wallet;
import com.orchexpay.walletledger.enums.WalletType;
import com.orchexpay.walletledger.models.Currency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateWalletUseCase {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Creates a wallet with default type MAIN (one per merchant per currency if using type-aware uniqueness).
     */
    @Transactional
    public Wallet execute(UUID merchantId, String currencyCode) {
        return execute(merchantId, currencyCode, WalletType.MAIN, null);
    }

    /**
     * Creates a wallet with the given type. For VENDOR, vendorUserId must be non-null and the user must be a vendor under this merchant.
     */
    @Transactional
    public Wallet execute(UUID merchantId, String currencyCode, WalletType walletType, UUID vendorUserId) {
        if (walletType == WalletType.VENDOR && vendorUserId == null) {
            throw new IllegalArgumentException("Vendor user id is required for VENDOR wallet type");
        }
        if (walletType != WalletType.VENDOR && vendorUserId != null) {
            throw new IllegalArgumentException("Vendor user id must be null for MAIN and ESCROW wallets");
        }
        if (walletType == WalletType.VENDOR) {
            userRepository.findById(vendorUserId)
                    .filter(u -> u.hasRole(Role.VENDOR) && merchantId.equals(u.getMerchantId()))
                    .orElseThrow(() -> new IllegalArgumentException("Vendor user not found or does not belong to this merchant"));
        }
        if (walletRepository.existsByMerchantIdAndCurrencyCodeAndWalletTypeAndVendorUserId(merchantId, currencyCode, walletType, vendorUserId)) {
            throw new WalletAlreadyExistsException(
                    "Wallet already exists for merchant " + merchantId + ", currency " + currencyCode + ", type " + walletType + (vendorUserId != null ? ", vendor " + vendorUserId : ""));
        }
        Currency currency = Currency.of(currencyCode);
        Instant now = Instant.now();
        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .walletType(walletType)
                .vendorUserId(vendorUserId)
                .currencyCode(currency.getCode())
                .status(com.orchexpay.walletledger.enums.WalletStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        wallet = walletRepository.save(wallet);
        eventPublisher.publish(WalletCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .walletId(wallet.getId())
                .merchantId(wallet.getMerchantId())
                .currencyCode(wallet.getCurrency().getCode())
                .occurredAt(Instant.now())
                .correlationId(null) // set by infrastructure from MDC/header
                .build());
        log.info("Created wallet {} for merchant {} type {} vendor {}", wallet.getId(), merchantId, walletType, vendorUserId);
        return wallet;
    }
}
