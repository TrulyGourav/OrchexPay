# OrchexPay

Event-driven digital wallet & payout orchestration platform.

## Services

| Service | Purpose |
|---------|---------|
| user-wallet-service | Users, wallet management, double-entry ledger, balance derivation |
| payout-orchestrator-service | Idempotent payout processing, state machine |
| risk-search-service | Fraud event consumption, Elasticsearch search |

## Tech Stack

- Java 17, Spring Boot 3.2.x
- PostgreSQL 15, Redis 7, Apache Kafka 3.6
- Elasticsearch 8.x (risk-search-service only)
- Docker, Micrometer/Prometheus

## Quick Start

```bash
docker-compose up -d
# Run each service: see service README
```

## Architecture

- **Strong consistency** within each service
- **Eventual consistency** across services via Kafka
- Outbox pattern for reliable event publishing
- JWT RBAC, idempotency via Redis + DB
