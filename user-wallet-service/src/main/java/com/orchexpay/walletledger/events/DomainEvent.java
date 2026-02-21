package com.orchexpay.walletledger.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker for domain events published to Kafka.
 * All events should carry correlationId for tracing.
 */
public interface DomainEvent {

    UUID getEventId();
    String getAggregateType();
    String getAggregateId();
    String getEventType();
    Instant getOccurredAt();
    String getCorrelationId();
}
