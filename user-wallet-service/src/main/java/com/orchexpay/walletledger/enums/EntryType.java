package com.orchexpay.walletledger.enums;

/**
 * Double-entry ledger: every transaction has DEBIT and CREDIT sides.
 * From wallet perspective: CREDIT increases balance, DEBIT decreases it.
 */
public enum EntryType {
    CREDIT,
    DEBIT
}
