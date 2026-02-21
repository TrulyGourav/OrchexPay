package com.orchexpay.payoutorchestrator.enums;

/**
 * Payout lifecycle. Orchestrator owns this state machine; wallet-service never sees it.
 * CREATED → PROCESSING → SETTLED | FAILED
 */
public enum PayoutStatus {
    CREATED,
    PROCESSING,
    SETTLED,
    FAILED
}
