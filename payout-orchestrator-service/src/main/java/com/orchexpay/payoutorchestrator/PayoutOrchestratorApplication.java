package com.orchexpay.payoutorchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Payout orchestration service: owns payout lifecycle, idempotency, bank calls, webhooks.
 * Must NEVER: modify wallet DB directly, calculate balance, or create ledger entries.
 * All balance/ledger effects MUST go via user-wallet-service APIs (reserve/credit/debit/confirm/reverse).
 * See docs/ARCHITECTURE_AUDIT.md for service boundaries.
 */
@SpringBootApplication
public class PayoutOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayoutOrchestratorApplication.class, args);
    }
}
