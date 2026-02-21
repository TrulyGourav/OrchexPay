package com.orchexpay.payoutorchestrator.repositories;

import com.orchexpay.payoutorchestrator.models.PendingOrder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PendingOrderRepository extends JpaRepository<PendingOrder, UUID> {

    List<PendingOrder> findByMerchantIdAndVendorIdAndSplitDoneFalseOrderByCreatedAtDesc(UUID merchantId, UUID vendorId);

    Optional<PendingOrder> findByMerchantIdAndOrderId(UUID merchantId, String orderId);
}
