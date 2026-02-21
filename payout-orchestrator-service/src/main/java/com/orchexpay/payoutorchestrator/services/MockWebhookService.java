package com.orchexpay.payoutorchestrator.services;

import com.orchexpay.payoutorchestrator.clients.WalletServiceClient;
import com.orchexpay.payoutorchestrator.models.PendingOrder;
import com.orchexpay.payoutorchestrator.repositories.PendingOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Handles mock payment and order-complete webhooks: credits escrow, then distributes by commission.
 * Tracks pending orders (payment success but not split) per vendor for UI.
 */
@Service
public class MockWebhookService {

    private static final Logger log = LoggerFactory.getLogger(MockWebhookService.class);

    private final WalletServiceClient walletServiceClient;
    private final CommissionService commissionService;
    private final PendingOrderRepository pendingOrderRepository;

    public MockWebhookService(WalletServiceClient walletServiceClient, CommissionService commissionService,
                              PendingOrderRepository pendingOrderRepository) {
        this.walletServiceClient = walletServiceClient;
        this.commissionService = commissionService;
        this.pendingOrderRepository = pendingOrderRepository;
    }

    /**
     * Simulates payment gateway success: credit merchant ESCROW and record pending order for vendor. Idempotent by orderId.
     * When requestBearerToken is present, it is forwarded to wallet-service so the credit is authorized as that user (e.g. MERCHANT).
     */
    public void handlePaymentSuccess(UUID merchantId, UUID vendorId, String orderId, BigDecimal amount, String currencyCode, UUID escrowWalletId, java.util.Optional<String> requestBearerToken) {
        UUID walletId = escrowWalletId != null ? escrowWalletId : walletServiceClient.getWalletByType(merchantId, currencyCode, "ESCROW", null);
        String idempotencyKey = "mock-payment-" + orderId;
        walletServiceClient.creditWallet(
                walletId,
                amount,
                currencyCode,
                orderId,
                "ORDER",
                "Mock payment for order " + orderId,
                idempotencyKey,
                requestBearerToken != null ? requestBearerToken : java.util.Optional.empty());

        Instant now = Instant.now();
        pendingOrderRepository.findByMerchantIdAndOrderId(merchantId, orderId)
                .ifPresentOrElse(
                        existing -> {
                            existing.setVendorId(vendorId);
                            existing.setAmount(amount);
                            existing.setCurrencyCode(currencyCode);
                            pendingOrderRepository.save(existing);
                            log.info("Mock payment success (updated pending): order {} vendor {} amount {}", orderId, vendorId, amount);
                        },
                        () -> {
                            PendingOrder pending = new PendingOrder();
                            pending.setId(UUID.randomUUID());
                            pending.setMerchantId(merchantId);
                            pending.setVendorId(vendorId);
                            pending.setOrderId(orderId);
                            pending.setAmount(amount);
                            pending.setCurrencyCode(currencyCode);
                            pending.setSplitDone(false);
                            pending.setCreatedAt(now);
                            pendingOrderRepository.save(pending);
                            log.info("Mock payment success: order {} amount {} credited to escrow {}, pending order recorded for vendor {}", orderId, amount, walletId, vendorId);
                        }
                );
    }

    /**
     * Simulates order completion: distribute ESCROW to VENDOR and MAIN by commission. Idempotent by orderId-split.
     * Marks the pending order as splitDone. When requestBearerToken is present, forwards it so wallet-service authorizes as that user (e.g. MERCHANT).
     */
    public void handleOrderComplete(UUID merchantId, String orderId, BigDecimal amount, String currencyCode,
                                    UUID vendorId, UUID escrowWalletId, UUID mainWalletId, UUID vendorWalletId,
                                    java.util.Optional<String> requestBearerToken) {
        UUID escrow = escrowWalletId != null ? escrowWalletId : walletServiceClient.getWalletByType(merchantId, currencyCode, "ESCROW", null);
        UUID main = mainWalletId != null ? mainWalletId : walletServiceClient.getWalletByType(merchantId, currencyCode, "MAIN", null);
        UUID vendor = vendorWalletId != null ? vendorWalletId : walletServiceClient.getWalletByType(merchantId, currencyCode, "VENDOR", vendorId);

        BigDecimal platformShare = commissionService.computePlatformShare(merchantId, amount, currencyCode);
        BigDecimal vendorShare = amount.subtract(platformShare).setScale(4, java.math.RoundingMode.HALF_UP);

        String referenceId = orderId + "-split";
        String idempotencyKey = "mock-split-" + orderId;
        List<WalletServiceClient.TransferLeg> legs = List.of(
                new WalletServiceClient.TransferLeg(vendor, vendorShare),
                new WalletServiceClient.TransferLeg(main, platformShare)
        );
        walletServiceClient.transfer(escrow, referenceId, currencyCode, amount, legs, "Order split " + orderId, idempotencyKey,
                requestBearerToken != null ? requestBearerToken : java.util.Optional.empty());

        pendingOrderRepository.findByMerchantIdAndOrderId(merchantId, orderId)
                .ifPresent(p -> {
                    p.setSplitDone(true);
                    pendingOrderRepository.save(p);
                });

        log.info("Mock order complete: order {} split vendor {} platform {}", orderId, vendorShare, platformShare);
    }

    public List<PendingOrder> listPendingOrdersByVendor(UUID merchantId, UUID vendorId) {
        return pendingOrderRepository.findByMerchantIdAndVendorIdAndSplitDoneFalseOrderByCreatedAtDesc(merchantId, vendorId);
    }
}
