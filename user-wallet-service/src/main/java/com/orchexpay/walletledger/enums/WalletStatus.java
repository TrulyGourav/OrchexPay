package com.orchexpay.walletledger.enums;

public enum WalletStatus {
    ACTIVE,
    FROZEN,   // admin freeze; no debit/credit until unfrozen
    SUSPENDED,
    CLOSED
}
