package com.orchexpay.walletledger.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final SecretKey key;
    private final String rolesClaim;
    private final long expirationSeconds;

    public JwtService(
            @Value("${orchexpay.jwt.secret:default-secret-min-256-bits-for-hs256-please-change-in-production-xyz}") String secret,
            @Value("${orchexpay.jwt.roles-claim:roles}") String rolesClaim,
            @Value("${orchexpay.jwt.expiration-seconds:86400}") long expirationSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.rolesClaim = rolesClaim;
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(String subject, List<String> roles) {
        return generateToken(subject, roles, null);
    }

    public String generateToken(String subject, List<String> roles, java.util.UUID merchantId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);
        var builder = Jwts.builder()
                .subject(subject)
                .claim(rolesClaim, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry));
        if (merchantId != null) {
            builder.claim("merchantId", merchantId.toString());
        }
        return builder.signWith(key).compact();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getSubject(String token) {
        return getClaims(token).map(Claims::getSubject).orElse(null);
    }

    public List<String> getRoles(String token) {
        return getClaims(token)
                .map(c -> {
                    Object rolesObj = c.get(rolesClaim);
                    if (rolesObj instanceof List<?> list) {
                        return list.stream().map(String::valueOf).collect(Collectors.toList());
                    }
                    return Collections.<String>emptyList();
                })
                .orElse(Collections.emptyList());
    }

    public Optional<java.util.UUID> getMerchantId(String token) {
        return getClaims(token)
                .map(c -> c.get("merchantId", String.class))
                .filter(id -> id != null && !id.isBlank())
                .map(java.util.UUID::fromString);
    }

    private Optional<Claims> getClaims(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
