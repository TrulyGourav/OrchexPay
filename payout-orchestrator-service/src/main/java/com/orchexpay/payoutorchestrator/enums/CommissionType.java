package com.orchexpay.payoutorchestrator.enums;

/**
 * Commission calculation model. Scalable: add new types (e.g. TIERED) without changing existing logic.
 */
public enum CommissionType {
    /** Platform share = orderAmount * (percentageValue / 100) */
    PERCENTAGE,
    /** Platform share = fixedAmount + (orderAmount * (percentageValue / 100)) */
    FIXED_PLUS_PERCENTAGE
}
