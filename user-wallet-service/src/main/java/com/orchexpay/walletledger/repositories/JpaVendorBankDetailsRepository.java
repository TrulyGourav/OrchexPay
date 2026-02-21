package com.orchexpay.walletledger.repositories;

import com.orchexpay.walletledger.models.VendorBankDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaVendorBankDetailsRepository extends JpaRepository<VendorBankDetails, UUID> {

    Optional<VendorBankDetails> findByUserId(UUID userId);
}
