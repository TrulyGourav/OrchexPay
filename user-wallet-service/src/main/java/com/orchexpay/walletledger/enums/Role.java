package com.orchexpay.walletledger.enums;

/**
 * Role hierarchy: SYSTEM &gt; ADMIN &gt; MERCHANT &gt; VENDOR.
 * MERCHANT = platform (e.g. FoodHub); VENDOR = seller under a merchant (e.g. PizzaKing).
 * Used for RBAC and method-level security.
 */
public enum Role {
    MERCHANT,
    VENDOR,
    ADMIN,
    SYSTEM
}
