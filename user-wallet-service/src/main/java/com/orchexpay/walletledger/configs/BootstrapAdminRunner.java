package com.orchexpay.walletledger.configs;

import com.orchexpay.walletledger.repositories.UserRepository;
import com.orchexpay.walletledger.enums.Role;
import com.orchexpay.walletledger.models.User;
import com.orchexpay.walletledger.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Creates bootstrap admin user (admin/password) on first startup only if not present.
 * Optionally creates SYSTEM user for payout-orchestrator service-to-service auth when orchexpay.bootstrap.system-user.enabled=true.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "password";
    private static final UUID ADMIN_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final String SYSTEM_USERNAME = "system";
    private static final UUID SYSTEM_ID = UUID.fromString("a0000000-0000-0000-0000-000000000002");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${orchexpay.bootstrap.system-user.enabled:false}")
    private boolean systemUserEnabled;

    @org.springframework.beans.factory.annotation.Value("${orchexpay.bootstrap.system-user.password:system}")
    private String systemUserPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByUsername(ADMIN_USERNAME)) {
            String passwordHash = passwordEncoder.encode(ADMIN_PASSWORD);
            Instant now = Instant.now();
            User admin = User.builder()
                    .id(ADMIN_ID)
                    .username(ADMIN_USERNAME)
                    .passwordHash(passwordHash)
                    .roles(Set.of(Role.ADMIN))
                    .merchantId(null)
                    .status(UserStatus.ACTIVE)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            userRepository.save(admin);
            log.info("Bootstrap admin user created (username={}). Change password in production.", ADMIN_USERNAME);
        }
        if (systemUserEnabled && !userRepository.existsByUsername(SYSTEM_USERNAME)) {
            String passwordHash = passwordEncoder.encode(systemUserPassword);
            Instant now = Instant.now();
            User system = User.builder()
                    .id(SYSTEM_ID)
                    .username(SYSTEM_USERNAME)
                    .passwordHash(passwordHash)
                    .roles(Set.of(Role.SYSTEM))
                    .merchantId(null)
                    .status(UserStatus.ACTIVE)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            userRepository.save(system);
            log.info("Bootstrap SYSTEM user created (username={}). Use for payout-orchestrator Bearer token.", SYSTEM_USERNAME);
        }
    }
}
