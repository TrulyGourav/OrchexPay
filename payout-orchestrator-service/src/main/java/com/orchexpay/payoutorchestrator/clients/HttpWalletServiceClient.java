package com.orchexpay.payoutorchestrator.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * HTTP client to user-wallet-service. All ledger effects go through this; no direct DB access to wallet/ledger.
 */
@Component
public class HttpWalletServiceClient implements WalletServiceClient {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final RestTemplate restTemplate;
    private final String walletServiceBaseUrl;

    public HttpWalletServiceClient(RestTemplate restTemplate,
                                   @Value("${orchexpay.wallet-service.url:http://localhost:8080}") String walletServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.walletServiceBaseUrl = walletServiceBaseUrl;
    }

    @Override
    public LedgerEntryResponse creditWallet(UUID walletId, BigDecimal amount, String currencyCode, String referenceId, String referenceType, String description, String idempotencyKey, Optional<String> requestBearerToken) {
        var headers = new HttpHeaders();
        headers.set(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        requestBearerToken.filter(t -> t != null && !t.isBlank()).ifPresent(t -> headers.set(HttpHeaders.AUTHORIZATION, t.startsWith("Bearer ") ? t : "Bearer " + t));
        var body = Map.<String, Object>of(
                "amount", amount,
                "currencyCode", currencyCode,
                "referenceId", referenceId,
                "referenceType", referenceType != null ? referenceType : "ORDER",
                "description", description != null ? description : ""
        );
        var response = restTemplate.postForEntity(
                walletServiceBaseUrl + "/api/v1/wallets/" + walletId + "/credit",
                new HttpEntity<>(body, headers),
                Map.class);
        return mapToLedgerEntryResponse(response.getBody());
    }

    @Override
    public LedgerEntryResponse reserveWallet(UUID walletId, BigDecimal amount, String currencyCode, String referenceId, String description, String idempotencyKey, Optional<String> requestBearerToken) {
        var headers = new HttpHeaders();
        headers.set(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        requestBearerToken.filter(t -> t != null && !t.isBlank()).ifPresent(t -> headers.set(HttpHeaders.AUTHORIZATION, t.startsWith("Bearer ") ? t : "Bearer " + t));
        var body = Map.<String, Object>of(
                "amount", amount,
                "currencyCode", currencyCode,
                "referenceId", referenceId,
                "description", description != null ? description : ""
        );
        var response = restTemplate.postForEntity(
                walletServiceBaseUrl + "/api/v1/wallets/" + walletId + "/reserve",
                new HttpEntity<>(body, headers),
                Map.class);
        return mapToLedgerEntryResponse(response.getBody());
    }

    @Override
    public void confirmLedgerEntry(UUID entryId, String idempotencyKey) {
        var headers = new HttpHeaders();
        headers.set(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
        restTemplate.postForEntity(
                walletServiceBaseUrl + "/api/v1/ledger-entries/" + entryId + "/confirm",
                new HttpEntity<>(headers),
                Map.class);
    }

    @Override
    public void reverseLedgerEntry(UUID entryId, String idempotencyKey) {
        var headers = new HttpHeaders();
        headers.set(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
        restTemplate.postForEntity(
                walletServiceBaseUrl + "/api/v1/ledger-entries/" + entryId + "/reverse",
                new HttpEntity<>(headers),
                Map.class);
    }

    @Override
    public TransferResultResponse transfer(UUID fromWalletId, String referenceId, String currencyCode, BigDecimal totalAmount, List<TransferLeg> legs, String description, String idempotencyKey, Optional<String> requestBearerToken) {
        var headers = new HttpHeaders();
        headers.set(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        requestBearerToken.filter(t -> t != null && !t.isBlank()).ifPresent(t -> headers.set(HttpHeaders.AUTHORIZATION, t.startsWith("Bearer ") ? t : "Bearer " + t));
        var creditLegs = legs.stream().map(l -> Map.<String, Object>of("toWalletId", l.toWalletId(), "amount", l.amount())).toList();
        var body = Map.<String, Object>of(
                "fromWalletId", fromWalletId,
                "referenceId", referenceId,
                "currencyCode", currencyCode,
                "totalAmount", totalAmount,
                "creditLegs", creditLegs,
                "description", description != null ? description : ""
        );
        var response = restTemplate.postForEntity(
                walletServiceBaseUrl + "/api/v1/transfers",
                new HttpEntity<>(body, headers),
                Map.class);
        var res = response.getBody();
        return new TransferResultResponse(res != null ? ((Map<?, ?>) res).get("debitEntry") : null,
                res != null ? (List<Object>) ((Map<?, ?>) res).get("creditEntries") : List.of(),
                res != null && Boolean.TRUE.equals(((Map<?, ?>) res).get("idempotent")));
    }

    @Override
    public UUID getWalletByType(UUID merchantId, String currencyCode, String walletType, UUID vendorUserId) {
        String url = walletServiceBaseUrl + "/api/v1/merchants/" + merchantId + "/wallets/by-type?currencyCode=" + currencyCode + "&walletType=" + walletType;
        if (vendorUserId != null) {
            url += "&vendorUserId=" + vendorUserId;
        }
        var response = restTemplate.getForEntity(url, Map.class);
        var body = response.getBody();
        if (body == null || !body.containsKey("id")) {
            throw new IllegalStateException("Wallet not found for merchant " + merchantId + " type " + walletType);
        }
        Object id = body.get("id");
        return id instanceof String ? UUID.fromString((String) id) : UUID.fromString(id.toString());
    }

    private static LedgerEntryResponse mapToLedgerEntryResponse(Map<?, ?> m) {
        if (m == null) return null;
        Object id = m.get("id");
        Object walletId = m.get("walletId");
        Object amount = m.get("amount");
        return new LedgerEntryResponse(
                id instanceof String ? UUID.fromString((String) id) : UUID.fromString(id.toString()),
                walletId instanceof String ? UUID.fromString((String) walletId) : UUID.fromString(walletId.toString()),
                (String) m.get("type"),
                amount instanceof BigDecimal ? (BigDecimal) amount : new BigDecimal(amount.toString()),
                (String) m.get("currencyCode"),
                (String) m.get("referenceType"),
                (String) m.get("referenceId"),
                (String) m.get("status"));
    }
}
