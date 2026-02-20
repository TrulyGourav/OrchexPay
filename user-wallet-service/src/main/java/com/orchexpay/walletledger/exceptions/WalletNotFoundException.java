package com.orchexpay.walletledger.exceptions;

import java.util.UUID;

public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(UUID walletId) {
        super("Wallet not found: " + walletId);
    }

    public WalletNotFoundException(String message) {
        super(message);
    }
}
