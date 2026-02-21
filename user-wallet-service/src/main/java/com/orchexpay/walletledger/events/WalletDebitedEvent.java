package com.orchexpay.walletledger.events;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class WalletDebitedEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID walletId;
    private final BigDecimal amount;
    private final String currencyCode;
    private final String referenceId;
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
        return "WalletDebited";
    }
}
