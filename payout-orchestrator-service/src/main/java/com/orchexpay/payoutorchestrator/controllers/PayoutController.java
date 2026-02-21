package com.orchexpay.payoutorchestrator.controllers;

import com.orchexpay.payoutorchestrator.models.PendingOrder;
import com.orchexpay.payoutorchestrator.models.Payout;
import com.orchexpay.payoutorchestrator.enums.PayoutStatus;
import com.orchexpay.payoutorchestrator.repositories.PayoutRepository;
import com.orchexpay.payoutorchestrator.services.MockWebhookService;
import com.orchexpay.payoutorchestrator.services.PayoutOrchestrationService;
import com.orchexpay.payoutorchestrator.dtos.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Payout API. List/get require auth (current user from user-wallet GET /me). Create with full body or POST /request (vendor self-service).
 */
@RestController
@RequestMapping("/api/v1/payouts")
public class PayoutController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String CURRENT_USER_ATTR = "currentUser";

    private final PayoutOrchestrationService orchestrationService;
    private final PayoutRepository payoutRepository;
    private final MockWebhookService mockWebhookService;

    public PayoutController(PayoutOrchestrationService orchestrationService, PayoutRepository payoutRepository,
                            MockWebhookService mockWebhookService) {
        this.orchestrationService = orchestrationService;
        this.payoutRepository = payoutRepository;
        this.mockWebhookService = mockWebhookService;
    }

    private static UserProfile requireCurrentUser(HttpServletRequest request) {
        UserProfile u = (UserProfile) request.getAttribute(CURRENT_USER_ATTR);
        if (u == null) {
            throw new IllegalStateException("Current user not resolved");
        }
        return u;
    }

    @GetMapping("/stats")
    public ResponseEntity<PayoutStatsResponse> stats(HttpServletRequest request) {
        UserProfile current = requireCurrentUser(request);
        if (!current.hasRole("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        long totalPayouts = payoutRepository.count();
        long createdCount = payoutRepository.countByStatus(PayoutStatus.CREATED);
        long processingCount = payoutRepository.countByStatus(PayoutStatus.PROCESSING);
        long settledCount = payoutRepository.countByStatus(PayoutStatus.SETTLED);
        long failedCount = payoutRepository.countByStatus(PayoutStatus.FAILED);
        BigDecimal totalSettledAmount = payoutRepository.sumAmountByStatus(PayoutStatus.SETTLED);
        if (totalSettledAmount == null) totalSettledAmount = BigDecimal.ZERO;
        return ResponseEntity.ok(new PayoutStatsResponse(totalPayouts, createdCount, processingCount, settledCount, failedCount, totalSettledAmount));
    }

    @GetMapping
    public ResponseEntity<Page<PayoutResponse>> list(
            @RequestParam(required = false) UUID vendorId,
            @RequestParam(required = false) UUID merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        UserProfile current = requireCurrentUser(request);
        boolean admin = current.hasRole("ADMIN");
        if (vendorId == null && merchantId == null) {
            if (!admin) {
                return ResponseEntity.badRequest().build();
            }
            Page<Payout> payouts = payoutRepository.findAll(
                    PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
            return ResponseEntity.ok(payouts.map(this::toResponse));
        }
        if (vendorId != null) {
            if (!admin && !current.getId().equals(vendorId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            Page<Payout> payouts = payoutRepository.findByVendorIdOrderByCreatedAtDesc(vendorId, PageRequest.of(page, Math.min(size, 100)));
            return ResponseEntity.ok(payouts.map(this::toResponse));
        } else {
            if (!admin && (current.getMerchantId() == null || !current.getMerchantId().equals(merchantId))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            Page<Payout> payouts = payoutRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, PageRequest.of(page, Math.min(size, 100)));
            return ResponseEntity.ok(payouts.map(this::toResponse));
        }
    }

    /**
     * List pending orders (payment success done, split not done) for a vendor under the merchant.
     * Merchant can only query their own merchantId.
     */
    @GetMapping("/pending-orders")
    public ResponseEntity<List<PendingOrderResponse>> listPendingOrders(
            @RequestParam UUID merchantId,
            @RequestParam UUID vendorId,
            HttpServletRequest request) {
        UserProfile current = requireCurrentUser(request);
        if (!current.hasRole("ADMIN") && (current.getMerchantId() == null || !current.getMerchantId().equals(merchantId))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<PendingOrder> list = mockWebhookService.listPendingOrdersByVendor(merchantId, vendorId);
        return ResponseEntity.ok(list.stream().map(this::toPendingOrderResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{payoutId}")
    public ResponseEntity<PayoutResponse> getById(@PathVariable UUID payoutId, HttpServletRequest request) {
        Payout payout = payoutRepository.findById(payoutId).orElse(null);
        if (payout == null) {
            return ResponseEntity.notFound().build();
        }
        UserProfile current = requireCurrentUser(request);
        boolean admin = current.hasRole("ADMIN");
        if (!admin && !payout.getVendorId().equals(current.getId()) && (current.getMerchantId() == null || !current.getMerchantId().equals(payout.getMerchantId()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(toResponse(payout));
    }

    @PostMapping("/request")
    public ResponseEntity<PayoutResponse> requestPayoutVendor(
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = true) String idempotencyKey,
            @Valid @RequestBody PayoutRequestVendor request,
            HttpServletRequest req) {
        UserProfile current = requireCurrentUser(req);
        if (current.getVendorWalletId() == null || current.getMerchantId() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null); // vendor wallet not found
        }
        String auth = req != null ? req.getHeader("Authorization") : null;
        Payout payout = orchestrationService.requestPayout(
                current.getMerchantId(), current.getId(), current.getVendorWalletId(),
                request.getAmount(), request.getCurrencyCode(), idempotencyKey,
                java.util.Optional.ofNullable(auth).filter(h -> h != null && !h.isBlank()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(payout));
    }

    @PostMapping
    public ResponseEntity<PayoutResponse> createPayout(
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = true) String idempotencyKey,
            @Valid @RequestBody PayoutRequest request,
            HttpServletRequest req) {
        String auth = req != null ? req.getHeader("Authorization") : null;
        Payout payout = orchestrationService.requestPayout(
                request.getMerchantId(), request.getVendorId(), request.getVendorWalletId(),
                request.getAmount(), request.getCurrencyCode(), idempotencyKey,
                java.util.Optional.ofNullable(auth).filter(h -> h != null && !h.isBlank()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(payout));
    }

    /**
     * Call after bank success (or simulate completion). Confirms PENDING ledger entry and sets payout SETTLED.
     * Allowed: ADMIN (any payout), MERCHANT (own merchant's payouts only). Vendor cannot confirm.
     */
    @PostMapping("/{payoutId}/confirm")
    public ResponseEntity<PayoutResponse> confirmPayout(
            @PathVariable UUID payoutId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = true) String idempotencyKey,
            HttpServletRequest request) {
        UserProfile current = requireCurrentUser(request);
        Payout payout = payoutRepository.findById(payoutId).orElse(null);
        if (payout == null) {
            return ResponseEntity.notFound().build();
        }
        if (!canConfirmOrReverse(current, payout)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String auth = request.getHeader("Authorization");
        payout = orchestrationService.confirmPayout(payoutId, idempotencyKey, java.util.Optional.ofNullable(auth).filter(h -> h != null && !h.isBlank()));
        return ResponseEntity.ok(toResponse(payout));
    }

    /**
     * Call after bank failure (or simulate failure). Reverses PENDING ledger entry and sets payout FAILED.
     * Allowed: ADMIN (any payout), MERCHANT (own merchant's payouts only). Vendor cannot reverse.
     */
    @PostMapping("/{payoutId}/reverse")
    public ResponseEntity<PayoutResponse> reversePayout(
            @PathVariable UUID payoutId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = true) String idempotencyKey,
            HttpServletRequest request) {
        UserProfile current = requireCurrentUser(request);
        Payout payout = payoutRepository.findById(payoutId).orElse(null);
        if (payout == null) {
            return ResponseEntity.notFound().build();
        }
        if (!canConfirmOrReverse(current, payout)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String auth = request.getHeader("Authorization");
        payout = orchestrationService.reversePayout(payoutId, idempotencyKey, java.util.Optional.ofNullable(auth).filter(h -> h != null && !h.isBlank()));
        return ResponseEntity.ok(toResponse(payout));
    }

    /** Only ADMIN or MERCHANT (for own merchant's payouts) can confirm/reverse. */
    private static boolean canConfirmOrReverse(UserProfile current, Payout payout) {
        if (current.hasRole("ADMIN")) return true;
        if (current.hasRole("MERCHANT") && current.getMerchantId() != null && current.getMerchantId().equals(payout.getMerchantId())) return true;
        return false;
    }

    private PendingOrderResponse toPendingOrderResponse(PendingOrder p) {
        return new PendingOrderResponse(p.getOrderId(), p.getAmount(), p.getCurrencyCode(), p.getCreatedAt());
    }

    private PayoutResponse toResponse(Payout p) {
        return PayoutResponse.builder()
                .id(p.getId())
                .merchantId(p.getMerchantId())
                .vendorId(p.getVendorId())
                .vendorWalletId(p.getVendorWalletId())
                .amount(p.getAmount())
                .currencyCode(p.getCurrencyCode())
                .status(p.getStatus().name())
                .ledgerEntryId(p.getLedgerEntryId())
                .createdAt(p.getCreatedAt())
                .build();
    }

    public static class PendingOrderResponse {
        private final String orderId;
        private final BigDecimal amount;
        private final String currencyCode;
        private final java.time.Instant createdAt;

        public PendingOrderResponse(String orderId, BigDecimal amount, String currencyCode, java.time.Instant createdAt) {
            this.orderId = orderId;
            this.amount = amount;
            this.currencyCode = currencyCode;
            this.createdAt = createdAt;
        }
        public String getOrderId() { return orderId; }
        public BigDecimal getAmount() { return amount; }
        public String getCurrencyCode() { return currencyCode; }
        public java.time.Instant getCreatedAt() { return createdAt; }
    }

    public static class PayoutRequest {
        private UUID merchantId;
        private UUID vendorId;
        private UUID vendorWalletId;
        private java.math.BigDecimal amount;
        private String currencyCode;
        public UUID getMerchantId() { return merchantId; }
        public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }
        public UUID getVendorId() { return vendorId; }
        public void setVendorId(UUID vendorId) { this.vendorId = vendorId; }
        public UUID getVendorWalletId() { return vendorWalletId; }
        public void setVendorWalletId(UUID vendorWalletId) { this.vendorWalletId = vendorWalletId; }
        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
        public String getCurrencyCode() { return currencyCode; }
        public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    }

    public static class PayoutRequestVendor {
        private java.math.BigDecimal amount;
        private String currencyCode;
        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
        public String getCurrencyCode() { return currencyCode; }
        public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    }

    public static class PayoutResponse {
        private UUID id;
        private UUID merchantId;
        private UUID vendorId;
        private UUID vendorWalletId;
        private java.math.BigDecimal amount;
        private String currencyCode;
        private String status;
        private UUID ledgerEntryId;
        private java.time.Instant createdAt;
        public static PayoutResponseBuilder builder() { return new PayoutResponseBuilder(); }
        public UUID getId() { return id; }
        public UUID getMerchantId() { return merchantId; }
        public UUID getVendorId() { return vendorId; }
        public UUID getVendorWalletId() { return vendorWalletId; }
        public java.math.BigDecimal getAmount() { return amount; }
        public String getCurrencyCode() { return currencyCode; }
        public String getStatus() { return status; }
        public UUID getLedgerEntryId() { return ledgerEntryId; }
        public java.time.Instant getCreatedAt() { return createdAt; }
        public static class PayoutResponseBuilder {
            private final PayoutResponse r = new PayoutResponse();
            public PayoutResponseBuilder id(UUID id) { r.id = id; return this; }
            public PayoutResponseBuilder merchantId(UUID v) { r.merchantId = v; return this; }
            public PayoutResponseBuilder vendorId(UUID v) { r.vendorId = v; return this; }
            public PayoutResponseBuilder vendorWalletId(UUID v) { r.vendorWalletId = v; return this; }
            public PayoutResponseBuilder amount(java.math.BigDecimal v) { r.amount = v; return this; }
            public PayoutResponseBuilder currencyCode(String v) { r.currencyCode = v; return this; }
            public PayoutResponseBuilder status(String v) { r.status = v; return this; }
            public PayoutResponseBuilder ledgerEntryId(UUID v) { r.ledgerEntryId = v; return this; }
            public PayoutResponseBuilder createdAt(java.time.Instant v) { r.createdAt = v; return this; }
            public PayoutResponse build() { return r; }
        }
    }

    public static class PayoutStatsResponse {
        private final long totalPayouts;
        private final long createdCount;
        private final long processingCount;
        private final long settledCount;
        private final long failedCount;
        private final BigDecimal totalSettledAmount;

        public PayoutStatsResponse(long totalPayouts, long createdCount, long processingCount, long settledCount, long failedCount, BigDecimal totalSettledAmount) {
            this.totalPayouts = totalPayouts;
            this.createdCount = createdCount;
            this.processingCount = processingCount;
            this.settledCount = settledCount;
            this.failedCount = failedCount;
            this.totalSettledAmount = totalSettledAmount != null ? totalSettledAmount : BigDecimal.ZERO;
        }
        public long getTotalPayouts() { return totalPayouts; }
        public long getCreatedCount() { return createdCount; }
        public long getProcessingCount() { return processingCount; }
        public long getSettledCount() { return settledCount; }
        public long getFailedCount() { return failedCount; }
        public BigDecimal getTotalSettledAmount() { return totalSettledAmount; }
    }
}
