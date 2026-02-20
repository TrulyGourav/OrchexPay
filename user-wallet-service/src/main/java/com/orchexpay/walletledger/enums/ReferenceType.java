package com.orchexpay.walletledger.enums;

/**
 * Business reference for a ledger entry. Used for idempotency and reconciliation.
 * ORDER = customer payment / escrow credit; PAYOUT = vendor payout; REFUND = customer refund; REVERSAL = compensating entry for failed payout.
 */
public enum ReferenceType {
    ORDER,
    PAYOUT,
    REFUND,
    REVERSAL
}
