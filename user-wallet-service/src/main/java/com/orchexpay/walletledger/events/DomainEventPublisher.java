package com.orchexpay.walletledger.events;

/**
 * Port for publishing domain events (Outbox pattern: write to outbox table, then poll/publish via Kafka).
 * Implementation persists to outbox and/or sends to Kafka.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
