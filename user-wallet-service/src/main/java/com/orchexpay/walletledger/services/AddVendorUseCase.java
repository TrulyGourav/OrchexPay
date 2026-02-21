package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.exceptions.UserAlreadyExistsException;
import com.orchexpay.walletledger.repositories.UserRepository;
import com.orchexpay.walletledger.enums.Role;
import com.orchexpay.walletledger.models.User;
import com.orchexpay.walletledger.enums.UserStatus;
import com.orchexpay.walletledger.models.Wallet;
import com.orchexpay.walletledger.enums.WalletType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Adds a vendor user under a merchant and creates their single VENDOR wallet in the same transaction.
 * Thread-safe: relies on DB unique constraints; constraint violations are converted to UserAlreadyExistsException.
 * Only the merchant (or ADMIN) should call this; authorization is enforced at API layer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddVendorUseCase {

    private static final String DEFAULT_VENDOR_CURRENCY = "INR";

    private final UserRepository userRepository;
    private final CreateWalletUseCase createWalletUseCase;
    private final PasswordEncoder passwordEncoder;

    @Transactional(rollbackFor = Exception.class)
    public AddVendorResult execute(UUID merchantId, String username, String password, String currencyCode) {
        String currency = (currencyCode != null && !currencyCode.isBlank())
                ? currencyCode.trim().toUpperCase()
                : DEFAULT_VENDOR_CURRENCY;
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException(username);
        }
        try {
            Instant now = Instant.now();
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username(username)
                    .passwordHash(passwordEncoder.encode(password))
                    .roles(Set.of(Role.VENDOR))
                    .merchantId(merchantId)
                    .status(UserStatus.ACTIVE)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            user = userRepository.save(user);

            Wallet vendorWallet = createWalletUseCase.execute(merchantId, currency, WalletType.VENDOR, user.getId());
            log.info("Added vendor {} under merchant {} with vendor wallet {}", username, merchantId, vendorWallet.getId());
            return new AddVendorResult(user, vendorWallet.getId());
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate vendor or wallet during add: {}", e.getMessage());
            throw new UserAlreadyExistsException(username);
        }
    }
}
