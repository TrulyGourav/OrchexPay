package com.orchexpay.walletledger.infrastructure.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Principal set after JWT validation. Holds username and optional merchantId (for MERCHANT/VENDOR users).
 * Used to authorize merchant-scoped actions (e.g. merchant can only add vendors to their own merchantId).
 */
public record LedgerPrincipal(String username, Optional<UUID> merchantId) {

    public boolean hasMerchantId(UUID merchantId) {
        return merchantId != null && this.merchantId.isPresent() && this.merchantId.get().equals(merchantId);
    }

    @Override
    public String toString() {
        return username;
    }
}
