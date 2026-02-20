package com.orchexpay.walletledger.configs;

import java.util.Optional;

/**
 * Port for idempotency key storage (Redis + DB fallback).
 * Returns existing response if key was already processed.
 */
public interface IdempotencyStore {

    /**
     * Try to claim the idempotency key. Returns empty if key is new and was claimed,
     * or the cached response if key was already processed.
     */
    Optional<String> getIfPresent(String idempotencyKey);

    /**
     * Store the response for the given idempotency key (after successful processing).
     */
    void put(String idempotencyKey, String responsePayload, long ttlSeconds);
}
