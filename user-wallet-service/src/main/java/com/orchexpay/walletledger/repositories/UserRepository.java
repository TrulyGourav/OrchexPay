package com.orchexpay.walletledger.repositories;

import com.orchexpay.walletledger.models.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for user persistence.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Page<User> findMerchantUsers(Pageable pageable);

    List<User> findVendorUsersByMerchantId(UUID merchantId);

    long countMerchantUsers();

    long countVendorUsers();
}
