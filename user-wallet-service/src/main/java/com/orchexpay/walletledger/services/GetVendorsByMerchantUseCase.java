package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.repositories.UserRepository;
import com.orchexpay.walletledger.repositories.WalletRepository;
import com.orchexpay.walletledger.models.User;
import com.orchexpay.walletledger.enums.WalletType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Returns vendors (user + vendorWalletId) for a merchant. Used by GET /api/v1/merchants/:id/vendors.
 */
@Service
@RequiredArgsConstructor
public class GetVendorsByMerchantUseCase {

    private static final String DEFAULT_CURRENCY = "INR";

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public List<VendorSummary> execute(UUID merchantId) {
        List<User> vendors = userRepository.findVendorUsersByMerchantId(merchantId);
        return vendors.stream()
                .flatMap(user -> {
                    var walletOpt = walletRepository.findByMerchantIdAndCurrencyCodeAndWalletTypeAndVendorUserId(
                            merchantId, DEFAULT_CURRENCY, WalletType.VENDOR, user.getId());
                    return walletOpt.map(w -> Stream.of(new VendorSummary(user.getId(), user.getUsername(), w.getId())))
                            .orElse(Stream.of(new VendorSummary(user.getId(), user.getUsername(), null)));
                })
                .toList();
    }

    public record VendorSummary(UUID userId, String username, UUID vendorWalletId) {}
}
