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
import java.util.stream.Collectors;

/**
 * Creates a user in a single transaction. Thread-safe: relies on DB unique constraints
 * (users.username, wallets natural key) and converts constraint violations to UserAlreadyExistsException.
 * <ul>
 *   <li>ADMIN / SYSTEM: user only, no wallets.</li>
 *   <li>MERCHANT: user + MAIN wallet + ESCROW wallet (currencyCode required).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateUserUseCase {

    private final UserRepository userRepository;
    private final CreateWalletUseCase createWalletUseCase;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a user and, for MERCHANT, creates MAIN and ESCROW wallets in the same transaction.
     * No wallets for ADMIN or SYSTEM.
     *
     * @param currencyCode required when roles contain MERCHANT; ignored otherwise
     */
    @Transactional(rollbackFor = Exception.class)
    public CreateUserResult execute(String username, String password, Set<Role> roles, String currencyCode) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required");
        }
        if (roles.contains(Role.VENDOR)) {
            throw new IllegalArgumentException("Vendors must be added by the merchant via POST /merchants/{merchantId}/vendors");
        }
        if (roles.contains(Role.MERCHANT) && (currencyCode == null || currencyCode.isBlank())) {
            throw new IllegalArgumentException("Currency code is required when creating a MERCHANT account");
        }
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException(username);
        }
        try {
            UUID merchantId = roles.contains(Role.MERCHANT) ? UUID.randomUUID() : null;
            String passwordHash = passwordEncoder.encode(password);
            Instant now = Instant.now();
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username(username)
                    .passwordHash(passwordHash)
                    .roles(roles)
                    .merchantId(merchantId)
                    .status(UserStatus.ACTIVE)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            user = userRepository.save(user);

            UUID mainWalletId = null;
            UUID escrowWalletId = null;
            if (roles.contains(Role.MERCHANT) && merchantId != null) {
                Wallet main = createWalletUseCase.execute(merchantId, currencyCode.trim().toUpperCase(), WalletType.MAIN, null);
                Wallet escrow = createWalletUseCase.execute(merchantId, currencyCode.trim().toUpperCase(), WalletType.ESCROW, null);
                mainWalletId = main.getId();
                escrowWalletId = escrow.getId();
            }

            log.info("Created user {} with roles {} merchantId={} wallets={}",
                    username, roles.stream().map(Enum::name).collect(Collectors.joining(",")), merchantId,
                    mainWalletId != null ? "MAIN,ESCROW" : "none");
            return new CreateUserResult(user, mainWalletId, escrowWalletId);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate user or wallet during create: {}", e.getMessage());
            throw new UserAlreadyExistsException(username);
        }
    }
}
