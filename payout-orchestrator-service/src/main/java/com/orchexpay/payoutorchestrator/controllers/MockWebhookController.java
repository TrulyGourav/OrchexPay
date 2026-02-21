package com.orchexpay.payoutorchestrator.controllers;

import com.orchexpay.payoutorchestrator.services.MockWebhookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Mock webhooks to simulate real payment flow: payment success → credit ESCROW; order complete → distribute by commission.
 * For testing and demos only.
 */
@RestController
@RequestMapping("/api/v1/mock/webhooks")
public class MockWebhookController {

    private final MockWebhookService mockWebhookService;

    public MockWebhookController(MockWebhookService mockWebhookService) {
        this.mockWebhookService = mockWebhookService;
    }

    /**
     * Simulates payment gateway success webhook. Credits merchant ESCROW. Idempotent by orderId.
     * Forwards request Authorization to wallet-service so the credit is performed as the authenticated merchant.
     */
    @PostMapping("/payment-success")
    public ResponseEntity<MockWebhookResponse> paymentSuccess(@Valid @RequestBody PaymentSuccessRequest request, HttpServletRequest httpRequest) {
        String auth = httpRequest != null ? httpRequest.getHeader("Authorization") : null;
        mockWebhookService.handlePaymentSuccess(
                request.getMerchantId(),
                request.getVendorId(),
                request.getOrderId(),
                request.getAmount(),
                request.getCurrencyCode(),
                request.getEscrowWalletId(),
                Optional.ofNullable(auth).filter(h -> !h.isBlank()));
        return ResponseEntity.status(HttpStatus.OK).body(
                new MockWebhookResponse("payment_success", "Escrow credited for order " + request.getOrderId()));
    }

    /**
     * Simulates order completion webhook. Distributes total amount: vendor share + platform (commission) share. Idempotent by orderId-split.
     * Forwards request Authorization to wallet-service so the transfer is performed as the authenticated merchant.
     */
    @PostMapping("/order-complete")
    public ResponseEntity<MockWebhookResponse> orderComplete(@Valid @RequestBody OrderCompleteRequest request, HttpServletRequest httpRequest) {
        String auth = httpRequest != null ? httpRequest.getHeader("Authorization") : null;
        mockWebhookService.handleOrderComplete(
                request.getMerchantId(),
                request.getOrderId(),
                request.getAmount(),
                request.getCurrencyCode(),
                request.getVendorId(),
                request.getEscrowWalletId(),
                request.getMainWalletId(),
                request.getVendorWalletId(),
                Optional.ofNullable(auth).filter(h -> !h.isBlank()));
        return ResponseEntity.status(HttpStatus.OK).body(
                new MockWebhookResponse("order_complete", "Order " + request.getOrderId() + " distributed by commission"));
    }

    public static class PaymentSuccessRequest {
        @NotNull
        private UUID merchantId;
        @NotNull
        private UUID vendorId;
        @NotBlank
        private String orderId;
        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal amount;
        @NotBlank
        @Pattern(regexp = "[A-Z]{3}")
        @Size(min = 3, max = 3)
        private String currencyCode;
        private UUID escrowWalletId;

        public UUID getMerchantId() { return merchantId; }
        public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }
        public UUID getVendorId() { return vendorId; }
        public void setVendorId(UUID vendorId) { this.vendorId = vendorId; }
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrencyCode() { return currencyCode; }
        public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
        public UUID getEscrowWalletId() { return escrowWalletId; }
        public void setEscrowWalletId(UUID escrowWalletId) { this.escrowWalletId = escrowWalletId; }
    }

    public static class OrderCompleteRequest {
        @NotNull
        private UUID merchantId;
        @NotBlank
        private String orderId;
        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal amount;
        @NotBlank
        @Pattern(regexp = "[A-Z]{3}")
        @Size(min = 3, max = 3)
        private String currencyCode;
        @NotNull
        private UUID vendorId;
        private UUID escrowWalletId;
        private UUID mainWalletId;
        private UUID vendorWalletId;

        public UUID getMerchantId() { return merchantId; }
        public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrencyCode() { return currencyCode; }
        public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
        public UUID getVendorId() { return vendorId; }
        public void setVendorId(UUID vendorId) { this.vendorId = vendorId; }
        public UUID getEscrowWalletId() { return escrowWalletId; }
        public void setEscrowWalletId(UUID escrowWalletId) { this.escrowWalletId = escrowWalletId; }
        public UUID getMainWalletId() { return mainWalletId; }
        public void setMainWalletId(UUID mainWalletId) { this.mainWalletId = mainWalletId; }
        public UUID getVendorWalletId() { return vendorWalletId; }
        public void setVendorWalletId(UUID vendorWalletId) { this.vendorWalletId = vendorWalletId; }
    }

    public static class MockWebhookResponse {
        private final String event;
        private final String message;

        public MockWebhookResponse(String event, String message) {
            this.event = event;
            this.message = message;
        }
        public String getEvent() { return event; }
        public String getMessage() { return message; }
    }
}
