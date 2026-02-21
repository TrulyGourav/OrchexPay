package com.orchexpay.walletledger.repositories;

import com.orchexpay.walletledger.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaUserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query(value = "SELECT * FROM users WHERE roles LIKE '%MERCHANT%' ORDER BY created_at DESC",
            countQuery = "SELECT COUNT(*) FROM users WHERE roles LIKE '%MERCHANT%'",
            nativeQuery = true)
    Page<User> findMerchantUsers(Pageable pageable);

    @Query(value = "SELECT * FROM users WHERE merchant_id = :merchantId AND roles LIKE '%VENDOR%' ORDER BY created_at DESC",
            nativeQuery = true)
    List<User> findVendorUsersByMerchantId(@Param("merchantId") UUID merchantId);

    @Query(value = "SELECT COUNT(*) FROM users WHERE roles LIKE '%MERCHANT%'", nativeQuery = true)
    long countMerchantUsers();

    @Query(value = "SELECT COUNT(*) FROM users WHERE roles LIKE '%VENDOR%'", nativeQuery = true)
    long countVendorUsers();
}
