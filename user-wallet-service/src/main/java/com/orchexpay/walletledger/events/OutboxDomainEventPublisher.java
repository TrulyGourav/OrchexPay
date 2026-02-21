package com.orchexpay.walletledger.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orchexpay.walletledger.models.OutboxEntity;
import com.orchexpay.walletledger.repositories.JpaOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes domain events to the outbox table in the same transaction as domain changes.
 * A separate process (scheduler or Kafka connector) reads from outbox and sends to Kafka.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final JpaOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(DomainEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", event.getEventId().toString());
        payload.put("aggregateType", event.getAggregateType());
        payload.put("aggregateId", event.getAggregateId());
        payload.put("eventType", event.getEventType());
        payload.put("occurredAt", event.getOccurredAt().toString());
        payload.put("correlationId", event.getCorrelationId());
        if (event instanceof WalletCreatedEvent wce) {
            payload.put("walletId", wce.getWalletId().toString());
            payload.put("merchantId", wce.getMerchantId().toString());
            payload.put("currencyCode", wce.getCurrencyCode());
        } else if (event instanceof WalletCreditedEvent wcre) {
            payload.put("walletId", wcre.getWalletId().toString());
            payload.put("amount", wcre.getAmount());
            payload.put("currencyCode", wcre.getCurrencyCode());
            payload.put("referenceId", wcre.getReferenceId());
        } else if (event instanceof WalletDebitedEvent wde) {
            payload.put("walletId", wde.getWalletId().toString());
            payload.put("amount", wde.getAmount());
            payload.put("currencyCode", wde.getCurrencyCode());
            payload.put("referenceId", wde.getReferenceId());
        }
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
        String correlationId = event.getCorrelationId() != null ? event.getCorrelationId() : MDC.get("correlationId");
        OutboxEntity outbox = OutboxEntity.builder()
                .id(UUID.randomUUID())
                .aggregateType(event.getAggregateType())
                .aggregateId(event.getAggregateId())
                .eventType(event.getEventType())
                .payload(payloadJson)
                .correlationId(correlationId)
                .createdAt(Instant.now())
                .published(false)
                .build();
        outboxRepository.save(outbox);
        log.debug("Outbox event saved: {} {}", event.getEventType(), event.getAggregateId());
    }
}
