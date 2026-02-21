package com.orchexpay.payoutorchestrator.services;

import com.orchexpay.payoutorchestrator.clients.WalletServiceClient;
import com.orchexpay.payoutorchestrator.models.Payout;
import com.orchexpay.payoutorchestrator.enums.PayoutStatus;
import com.orchexpay.payoutorchestrator.repositories.PayoutRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Orchestrates payout: create → reserve (wallet) → bank call (external) → confirm or reverse.
 * Owns payout state; never writes to wallet DB directly (all ledger effects via WalletServiceClient).
 * Real bank movement must only occur in this service (or its caller, e.g. bank webhook handler);
 * after bank success call confirmPayout; after bank failure call reversePayout.
 */
@Service
public class PayoutOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(PayoutOrchestrationService.class);

    private final PayoutRepository payoutRepository;
    private final PayoutStateMachine stateMachine;
    private final WalletServiceClient walletServiceClient;

    public PayoutOrchestrationService(PayoutRepository payoutRepository, PayoutStateMachine stateMachine, WalletServiceClient walletServiceClient) {
        this.payoutRepository = payoutRepository;
        this.stateMachine = stateMachine;
        this.walletServiceClient = walletServiceClient;
    }

    /**
     * Idempotent payout request. If idempotencyKey seen before, return existing payout.
     * Otherwise: create Payout CREATED, call wallet.reserve(), set PROCESSING, then caller invokes bank and later confirm/reverse.
     * When requestBearerToken is present (e.g. vendor self-service), it is forwarded to wallet-service so reserve is authorized as that user.
     */
    @Transactional
    public Payout requestPayout(UUID merchantId, UUID vendorId, UUID vendorWalletId,
                                java.math.BigDecimal amount, String currencyCode,
                                String idempotencyKey,
                                java.util.Optional<String> requestBearerToken) {
        var existing = payoutRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent payout: key {} already exists", idempotencyKey);
            return existing.get();
        }
        Payout payout = Payout.builder()
                .id(UUID.randomUUID())
                .merchantId(merchantId)
                .vendorId(vendorId)
                .vendorWalletId(vendorWalletId)
                .amount(amount)
                .currencyCode(currencyCode)
                .status(PayoutStatus.CREATED)
                .idempotencyKey(idempotencyKey)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        payout = payoutRepository.save(payout);

        stateMachine.toProcessing(payout);
        String referenceId = payout.getId().toString();
        var reserveResponse = walletServiceClient.reserveWallet(
                vendorWalletId, amount, currencyCode, referenceId, "Vendor payout " + payout.getId(), idempotencyKey + "-reserve",
                requestBearerToken != null ? requestBearerToken : java.util.Optional.empty());
        payout.setLedgerEntryId(reserveResponse.id());
        payout.setUpdatedAt(Instant.now());
        payoutRepository.save(payout);
        log.info("Payout {} reserved ledger entry {}", payout.getId(), reserveResponse.id());
        return payout;
    }

    /**
     * After bank success webhook: confirm the PENDING ledger entry, set payout SETTLED.
     * Idempotent: if payout already SETTLED, returns existing payout without calling wallet again.
     */
    @Transactional
    public Payout confirmPayout(UUID payoutId, String idempotencyKey, java.util.Optional<String> requestBearerToken) {
        Payout payout = payoutRepository.findById(payoutId).orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));
        if (payout.getStatus() == PayoutStatus.SETTLED) {
            log.info("Idempotent confirm: payout {} already SETTLED", payoutId);
            return payout;
        }
        if (payout.getLedgerEntryId() == null) {
            throw new IllegalStateException("Payout has no ledger entry to confirm");
        }
        walletServiceClient.confirmLedgerEntry(payout.getLedgerEntryId(), idempotencyKey, requestBearerToken != null ? requestBearerToken : java.util.Optional.empty());
        stateMachine.toSettled(payout);
        payout.setUpdatedAt(Instant.now());
        return payoutRepository.save(payout);
    }

    /**
     * After bank failure: reverse the PENDING ledger entry, set payout FAILED.
     * Idempotent: if payout already FAILED, returns existing payout without calling wallet again.
     */
    @Transactional
    public Payout reversePayout(UUID payoutId, String idempotencyKey, java.util.Optional<String> requestBearerToken) {
        Payout payout = payoutRepository.findById(payoutId).orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));
        if (payout.getStatus() == PayoutStatus.FAILED) {
            log.info("Idempotent reverse: payout {} already FAILED", payoutId);
            return payout;
        }
        if (payout.getLedgerEntryId() == null) {
            throw new IllegalStateException("Payout has no ledger entry to reverse");
        }
        walletServiceClient.reverseLedgerEntry(payout.getLedgerEntryId(), idempotencyKey, requestBearerToken != null ? requestBearerToken : java.util.Optional.empty());
        stateMachine.toFailed(payout);
        payout.setUpdatedAt(Instant.now());
        return payoutRepository.save(payout);
    }
}
