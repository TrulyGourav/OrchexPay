package com.orchexpay.payoutorchestrator.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RestTemplateConfig {

    @Bean("restTemplate")
    public RestTemplate restTemplate(
            @Value("${orchexpay.wallet-service.bearer-token:}") String walletServiceBearerToken) {
        RestTemplate rest = new RestTemplate();
        if (walletServiceBearerToken != null && !walletServiceBearerToken.isBlank()) {
            List<org.springframework.http.client.ClientHttpRequestInterceptor> interceptors =
                    new ArrayList<>(rest.getInterceptors());
            interceptors.add(new WalletServiceAuthInterceptor(walletServiceBearerToken.trim()));
            rest.setInterceptors(interceptors);
        }
        return rest;
    }

    /** Used by WalletServiceMeClient to call GET /me with per-request Bearer token (no global interceptor). */
    @Bean("restTemplateNoAuth")
    public RestTemplate restTemplateNoAuth() {
        return new RestTemplate();
    }
}
