package com.orchexpay.payoutorchestrator.services;

import com.orchexpay.payoutorchestrator.models.Payout;
import com.orchexpay.payoutorchestrator.enums.PayoutStatus;
import org.springframework.stereotype.Component;

/**
 * Explicit payout state transitions. Only valid transitions allowed.
 * CREATED → PROCESSING → SETTLED | FAILED
 */
@Component
public class PayoutStateMachine {

    public void toProcessing(Payout payout) {
        if (payout.getStatus() != PayoutStatus.CREATED) {
            throw new IllegalStateException("Only CREATED payouts can move to PROCESSING; current: " + payout.getStatus());
        }
        payout.setStatus(PayoutStatus.PROCESSING);
    }

    public void toSettled(Payout payout) {
        if (payout.getStatus() != PayoutStatus.PROCESSING) {
            throw new IllegalStateException("Only PROCESSING payouts can move to SETTLED; current: " + payout.getStatus());
        }
        payout.setStatus(PayoutStatus.SETTLED);
    }

    public void toFailed(Payout payout) {
        if (payout.getStatus() != PayoutStatus.PROCESSING) {
            throw new IllegalStateException("Only PROCESSING payouts can move to FAILED; current: " + payout.getStatus());
        }
        payout.setStatus(PayoutStatus.FAILED);
    }
}
