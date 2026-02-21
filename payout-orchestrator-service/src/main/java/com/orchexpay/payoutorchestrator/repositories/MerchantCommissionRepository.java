package com.orchexpay.payoutorchestrator.repositories;

import com.orchexpay.payoutorchestrator.models.MerchantCommission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MerchantCommissionRepository extends JpaRepository<MerchantCommission, UUID> {

    Optional<MerchantCommission> findByMerchantId(UUID merchantId);
}
