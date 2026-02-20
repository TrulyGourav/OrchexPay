package com.orchexpay.walletledger.enums;

/**
 * Lifecycle status of a ledger entry. Balance is derived only from CONFIRMED entries.
 * PENDING = reserved (e.g. payout initiated); CONFIRMED = settled; REVERSED = cancelled, compensating entry exists.
 */
public enum EntryStatus {
    PENDING,
    CONFIRMED,
    REVERSED
}
