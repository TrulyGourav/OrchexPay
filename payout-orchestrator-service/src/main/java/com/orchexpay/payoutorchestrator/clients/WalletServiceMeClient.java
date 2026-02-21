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

/**
 * Calls user-wallet-service GET /api/v1/users/me with the request's Bearer token to resolve current user.
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

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}
