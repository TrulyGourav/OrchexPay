package com.orchexpay.walletledger.events;

import com.orchexpay.walletledger.models.OutboxEntity;
import com.orchexpay.walletledger.repositories.JpaOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Relays outbox events to Kafka. Runs periodically; in production consider Kafka Connect or Debezium.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxKafkaRelay {

    private static final String TOPIC_WALLET_EVENTS = "ledgerx.wallet.events";
    private static final int BATCH_SIZE = 100;

    private final JpaOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${orchexpay.outbox.relay-interval-ms:5000}")
    @Transactional
    public void relay() {
        List<OutboxEntity> events = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE));
        for (OutboxEntity event : events) {
            try {
                kafkaTemplate.send(TOPIC_WALLET_EVENTS, event.getAggregateId(), event.getPayload());
                event.setPublished(true);
                outboxRepository.save(event);
                log.debug("Published outbox event {} to Kafka", event.getId());
            } catch (Exception e) {
                log.warn("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
                break; // retry next run
            }
        }
    }
}
