package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.exceptions.UserNotFoundException;
import com.orchexpay.walletledger.repositories.UserRepository;
import com.orchexpay.walletledger.repositories.WalletRepository;
import com.orchexpay.walletledger.enums.Role;
import com.orchexpay.walletledger.models.User;
import com.orchexpay.walletledger.enums.WalletType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Returns current user profile with wallet IDs (main, escrow for MERCHANT; vendor for VENDOR).
 * Used by GET /api/v1/users/me.
 */
@Service
@RequiredArgsConstructor
public class GetCurrentUserProfileUseCase {

    private static final String DEFAULT_CURRENCY = "INR";

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public CurrentUserProfile execute(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        UUID mainWalletId = null;
        UUID escrowWalletId = null;
        UUID vendorWalletId = null;

        if (user.getMerchantId() != null) {
            if (user.hasRole(Role.MERCHANT)) {
                mainWalletId = walletRepository
                        .findByMerchantIdAndCurrencyCodeAndWalletTypeAndVendorUserId(
                                user.getMerchantId(), DEFAULT_CURRENCY, WalletType.MAIN, null)
                        .map(w -> w.getId()).orElse(null);
                escrowWalletId = walletRepository
                        .findByMerchantIdAndCurrencyCodeAndWalletTypeAndVendorUserId(
                                user.getMerchantId(), DEFAULT_CURRENCY, WalletType.ESCROW, null)
                        .map(w -> w.getId()).orElse(null);
            }
            if (user.hasRole(Role.VENDOR)) {
                vendorWalletId = walletRepository
                        .findByMerchantIdAndCurrencyCodeAndWalletTypeAndVendorUserId(
                                user.getMerchantId(), DEFAULT_CURRENCY, WalletType.VENDOR, user.getId())
                        .map(w -> w.getId()).orElse(null);
            }
        }

        return new CurrentUserProfile(user, mainWalletId, escrowWalletId, vendorWalletId);
    }

    public record CurrentUserProfile(User user, UUID mainWalletId, UUID escrowWalletId, UUID vendorWalletId) {}
}
