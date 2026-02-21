package com.orchexpay.payoutorchestrator.repositories;

import com.orchexpay.payoutorchestrator.models.Payout;
import com.orchexpay.payoutorchestrator.enums.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PayoutRepository extends JpaRepository<Payout, UUID> {

    Optional<Payout> findByIdempotencyKey(String idempotencyKey);

    Page<Payout> findByVendorIdOrderByCreatedAtDesc(UUID vendorId, Pageable pageable);

    Page<Payout> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId, Pageable pageable);

    long countByStatus(PayoutStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payout p WHERE p.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PayoutStatus status);
}
