package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.models.User;

import java.util.UUID;

/**
 * Result of adding a vendor. Vendor user and their single VENDOR wallet are created in the same transaction.
 */
public record AddVendorResult(User user, UUID vendorWalletId) {}
