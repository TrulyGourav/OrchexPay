package com.orchexpay.walletledger.domain.model;

import com.orchexpay.walletledger.infrastructure.persistence.converter.RoleSetConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * User aggregate and JPA entity. Holds credentials (hashed) and roles for authentication and authorization.
 * Optional merchantId links MERCHANT users to a merchant entity.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true, length = 255)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "roles", nullable = false, length = 100)
    @Convert(converter = RoleSetConverter.class)
    private Set<Role> roles;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Ensures default values when building; called after load from DB for safety. */
    public void ensureDefaults() {
        if (id == null) id = UUID.randomUUID();
        if (roles == null) roles = Collections.emptySet();
        if (status == null) status = UserStatus.ACTIVE;
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }
}
