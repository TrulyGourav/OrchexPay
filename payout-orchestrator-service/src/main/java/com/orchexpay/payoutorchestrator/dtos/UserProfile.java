package com.orchexpay.payoutorchestrator.dtos;

import java.util.Set;
import java.util.UUID;

/**
 * Mirror of user-wallet-service UserResponse for GET /me. Used to resolve current user for auth in payout API.
 */
public class UserProfile {
    private UUID id;
    private String username;
    private Set<String> roles;
    private UUID merchantId;
    private UUID mainWalletId;
    private UUID escrowWalletId;
    private UUID vendorWalletId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }
    public UUID getMainWalletId() { return mainWalletId; }
    public void setMainWalletId(UUID mainWalletId) { this.mainWalletId = mainWalletId; }
    public UUID getEscrowWalletId() { return escrowWalletId; }
    public void setEscrowWalletId(UUID escrowWalletId) { this.escrowWalletId = escrowWalletId; }
    public UUID getVendorWalletId() { return vendorWalletId; }
    public void setVendorWalletId(UUID vendorWalletId) { this.vendorWalletId = vendorWalletId; }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
