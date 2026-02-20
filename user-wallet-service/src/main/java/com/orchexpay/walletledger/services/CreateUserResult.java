package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.models.User;

import java.util.UUID;

/**
 * Result of creating a user. For MERCHANT, mainWalletId and escrowWalletId are set (wallets created in same transaction).
 * For ADMIN/SYSTEM, both are null.
 */
public record CreateUserResult(User user, UUID mainWalletId, UUID escrowWalletId) {}
