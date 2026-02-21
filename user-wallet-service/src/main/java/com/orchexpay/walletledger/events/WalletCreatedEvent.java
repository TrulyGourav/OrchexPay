package com.orchexpay.walletledger.events;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class WalletCreatedEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID walletId;
    private final UUID merchantId;
    private final String currencyCode;
    private final Instant occurredAt;
    private final String correlationId;

    @Override
    public String getAggregateType() {
        return "Wallet";
    }

    @Override
    public String getAggregateId() {
        return walletId.toString();
    }

    @Override
    public String getEventType() {
        return "WalletCreated";
    }
}
