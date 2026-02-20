package com.orchexpay.walletledger.exceptions;

import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(UUID walletId) {
        super("Insufficient balance for wallet: " + walletId);
    }
}
