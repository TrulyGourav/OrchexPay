package com.orchexpay.walletledger.exceptions;

import java.util.UUID;

public class LedgerEntryNotFoundException extends RuntimeException {

    public LedgerEntryNotFoundException(UUID entryId) {
        super("Ledger entry not found: " + entryId);
    }
}
