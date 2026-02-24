package com.orchexpay.payoutorchestrator.clients;

import com.orchexpay.payoutorchestrator.dtos.UserProfile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Calls user-wallet-service GET /api/v1/users/me with the request's Bearer token to resolve current user.
 * Also supports GET /api/v1/users/{id} to resolve username by user id (for payout vendor display).
 */
@Component
public class WalletServiceMeClient {

    private final String walletServiceUrl;
    private final RestTemplate restTemplateNoAuth;

    public WalletServiceMeClient(
            @Value("${orchexpay.wallet-service.url:http://localhost:8080}") String walletServiceUrl,
            @Qualifier("restTemplateNoAuth") RestTemplate restTemplateNoAuth) {
        this.walletServiceUrl = walletServiceUrl.replaceAll("/$", "");
        this.restTemplateNoAuth = restTemplateNoAuth;
    }

    public UserProfile getMe(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new UnauthorizedException("Missing Authorization");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken.startsWith("Bearer ") ? bearerToken.substring(7) : bearerToken);
        try {
            return restTemplateNoAuth.exchange(
                    walletServiceUrl + "/api/v1/users/me",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    UserProfile.class
            ).getBody();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 401) {
                throw new UnauthorizedException("Invalid or expired token");
            }
            throw e;
        }
    }

    /** Resolve username by user id. Uses bearer token (merchant/admin) for auth. Returns null if 404/403. */
    @SuppressWarnings("unchecked")
    public String getUsername(UUID userId, String bearerToken) {
        if (userId == null || bearerToken == null || bearerToken.isBlank()) {
            return null;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, bearerToken.startsWith("Bearer ") ? bearerToken : "Bearer " + bearerToken);
        try {
            var response = restTemplateNoAuth.exchange(
                    walletServiceUrl + "/api/v1/users/" + userId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class);
            if (response.getBody() != null && response.getBody().containsKey("username")) {
                return (String) response.getBody().get("username");
            }
            return null;
        } catch (RestClientResponseException e) {
            return null;
        }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}
