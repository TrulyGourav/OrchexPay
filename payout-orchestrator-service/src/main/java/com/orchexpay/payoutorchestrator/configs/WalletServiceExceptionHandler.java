package com.orchexpay.payoutorchestrator.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * Maps errors from user-wallet-service (e.g. insufficient balance on reserve) to proper HTTP responses
 * so the UI can show a clear message instead of 500.
 */
@RestControllerAdvice
public class WalletServiceExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, String>> handleWalletServiceError(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        String body = ex.getResponseBodyAsString();
        String message = parseMessage(body);
        if (status == 422 && message != null && message.toLowerCase().contains("insufficient balance")) {
            message = "Insufficient balance. Please enter an amount not exceeding your available balance.";
        }
        if (message == null || message.isBlank()) {
            message = status == 422 ? "Insufficient balance. Please enter an amount not exceeding your available balance." : "Request failed.";
        }
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    private String parseMessage(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(body, Map.class);
            Object m = map != null ? map.get("message") : null;
            return m != null ? m.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
