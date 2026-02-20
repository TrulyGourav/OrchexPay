package com.orchexpay.walletledger.enums;

/**
 * Type of wallet for a merchant. Determines uniqueness and usage.
 * MAIN = platform's primary wallet; ESCROW = holds funds until order completion; VENDOR = sub-wallet for a vendor under the merchant.
 */
public enum WalletType {
    MAIN,
    ESCROW,
    VENDOR
}
