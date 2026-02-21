package com.orchexpay.payoutorchestrator.configs;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Adds Authorization: Bearer &lt;token&gt; when orchexpay.wallet-service.bearer-token is set.
 * Required for production: user-wallet-service secures /api/v1/**; orchestrator must send a valid JWT (e.g. SYSTEM user).
 */
public class WalletServiceAuthInterceptor implements ClientHttpRequestInterceptor {

    private final String bearerToken;

    public WalletServiceAuthInterceptor(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        if (StringUtils.hasText(bearerToken)) {
            request.getHeaders().setBearerAuth(bearerToken);
        }
        return execution.execute(request, body);
    }
}
