package com.orchexpay.payoutorchestrator.clients;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for wallet-service. Orchestrator calls these; no direct DB or ledger manipulation.
 * Implemented via HTTP (RestTemplate/WebClient) to user-wallet-service.
 */
public interface WalletServiceClient {

    /** Credit escrow on payment success. Idempotent by (walletId, referenceId, ORDER). When requestBearerToken is present, forwards it so wallet-service authorizes as that user (e.g. MERCHANT). */
    LedgerEntryResponse creditWallet(UUID walletId, BigDecimal amount, String currencyCode, String referenceId, String referenceType, String description, String idempotencyKey, Optional<String> requestBearerToken);

    /** @deprecated Use {@link #creditWallet(UUID, BigDecimal, String, String, String, String, String, Optional)} with empty token. */
    default LedgerEntryResponse creditWallet(UUID walletId, BigDecimal amount, String currencyCode, String referenceId, String referenceType, String description, String idempotencyKey) {
        return creditWallet(walletId, amount, currencyCode, referenceId, referenceType, description, idempotencyKey, Optional.empty());
    }

    /** Reserve vendor wallet (PENDING debit). Idempotent by (walletId, referenceId). When requestBearerToken is present, forwards it so wallet-service authorizes as that user (e.g. VENDOR). */
    LedgerEntryResponse reserveWallet(UUID walletId, BigDecimal amount, String currencyCode, String referenceId, String description, String idempotencyKey, Optional<String> requestBearerToken);

    /** @deprecated Use {@link #reserveWallet(UUID, BigDecimal, String, String, String, String, Optional)} with empty token. */
    default LedgerEntryResponse reserveWallet(UUID walletId, BigDecimal amount, String currencyCode, String referenceId, String description, String idempotencyKey) {
        return reserveWallet(walletId, amount, currencyCode, referenceId, description, idempotencyKey, Optional.empty());
    }

    /** Confirm PENDING entry after bank success. */
    void confirmLedgerEntry(UUID entryId, String idempotencyKey);

    /** Reverse PENDING entry on bank failure; creates compensating CREDIT. */
    void reverseLedgerEntry(UUID entryId, String idempotencyKey);

    /** Atomic transfer (e.g. ESCROW → VENDOR + MAIN). Idempotent by (fromWalletId, referenceId). When requestBearerToken is present, forwards it so wallet-service authorizes as that user (e.g. MERCHANT). */
    TransferResultResponse transfer(UUID fromWalletId, String referenceId, String currencyCode, BigDecimal totalAmount, List<TransferLeg> legs, String description, String idempotencyKey, Optional<String> requestBearerToken);

    /** @deprecated Use {@link #transfer(UUID, String, String, BigDecimal, List, String, String, Optional)} with empty token. */
    default TransferResultResponse transfer(UUID fromWalletId, String referenceId, String currencyCode, BigDecimal totalAmount, List<TransferLeg> legs, String description, String idempotencyKey) {
        return transfer(fromWalletId, referenceId, currencyCode, totalAmount, legs, description, idempotencyKey, Optional.empty());
    }

    /** Resolve wallet ID by merchant, currency, and type (ESCROW, MAIN, VENDOR). For VENDOR, vendorUserId is required. */
    UUID getWalletByType(UUID merchantId, String currencyCode, String walletType, UUID vendorUserId);

    record LedgerEntryResponse(UUID id, UUID walletId, String type, BigDecimal amount, String currencyCode, String referenceType, String referenceId, String status) {}
    record TransferLeg(UUID toWalletId, BigDecimal amount) {}
    record TransferResultResponse(Object debitEntry, List<Object> creditEntries, boolean idempotent) {}
}
