# FlowBridge Finance Tracker

> A production-grade **personal finance management platform** built on top of [FlowBridge](https://github.com/your-org/flowbridge) — a modular, pluggable event-bus library for Java/Spring Boot.

[![CI/CD](https://github.com/your-org/flowbridge-finance-tracker/actions/workflows/ci.yml/badge.svg)](https://github.com/your-org/flowbridge-finance-tracker/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen)
![FlowBridge](https://img.shields.io/badge/FlowBridge-1.0.0--SNAPSHOT-purple)

---

## Overview

FlowBridge Finance Tracker demonstrates how to build a fully event-driven microservice architecture using **FlowBridge** as the event bus layer. The platform supports:

- 🔐 **JWT Authentication** with role-based access control (ADMIN / USER)
- 💳 **Transaction Management** — record income and expenses by category
- 📊 **Budget Management** — configure per-category monthly/yearly limits with automatic exceed alerts
- 🌊 **FlowBridge Event Bus** — decoupled event publishing and consumption with pluggable providers (LOCAL, EMBEDDED via RocksDB, KAFKA)
- 🔁 **Admin Operations** — DLQ inspection, event replay, retry, and deletion
- 📈 **Observability** — Micrometer metrics, Prometheus scraping, and Grafana dashboards
- 🐳 **Dockerized** — full Docker Compose stack with PostgreSQL, Prometheus, and Grafana

---

## Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                    finance-tracker-api (Spring MVC)                │
│   REST Controllers │ Admin Dashboard (Thymeleaf) │ Actuator        │
└────────────┬───────────────────────────────────────────────────────┘
             │ uses
┌────────────▼───────────────────────┐
│     finance-tracker-application    │
│   Use Cases │ Port Interfaces       │
└────────────┬────────────┬──────────┘
             │            │
   ┌─────────▼──┐  ┌──────▼──────────────────────┐
   │  domain    │  │  finance-tracker-events      │
   │  models    │  │  Publishers │ Consumers      │
   └────────────┘  │  Payloads  │ Retry Logic    │
                   └──────┬──────────────────────┘
                          │ FlowBridge EventBus
                   ┌──────▼───────────────────────┐
                   │  FlowBridge Provider          │
                   │  LOCAL | EMBEDDED | KAFKA     │
                   └──────────────────────────────┘
   ┌────────────────────────────────┐
   │  finance-tracker-infrastructure│
   │  JPA │ Flyway │ Security │ JWT │
   └────────────────────────────────┘
```

---

## Module Structure

| Module | Description |
|---|---|
| `finance-tracker-domain` | Pure domain models: `User`, `Transaction`, `Budget`, `Role` |
| `finance-tracker-application` | Use cases and port interfaces (no framework dependencies) |
| `finance-tracker-infrastructure` | JPA entities, repositories, Flyway migrations, JWT, Security |
| `finance-tracker-events` | FlowBridge event payloads, publishers, consumers, retry logic |
| `finance-tracker-api` | Spring MVC controllers, Thymeleaf admin UI, actuator, entry point |
| `finance-tracker-tests` | Integration test suite (embedded PostgreSQL + FlowBridge) |

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker + Docker Compose (for full stack)
- PostgreSQL 16 (if running locally without Docker)

### Running Locally (No Docker)

```bash
# 1. Start PostgreSQL (adjust credentials as needed)
createdb finance_tracker

# 2. Build all modules
mvn clean package -DskipTests

# 3. Run the API
cd finance-tracker-api
mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="-DSPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/finance_tracker"
```

### Running with Docker Compose

```bash
# Copy and configure environment
cp .env.example .env   # edit DB_PASSWORD, JWT_SECRET as needed

# Build and start all services
docker compose up --build

# Services:
# • API:        http://localhost:8080
# • Actuator:   http://localhost:8081/actuator
# • Prometheus: http://localhost:9090
# • Grafana:    http://localhost:3000  (admin/admin)
```

---

## API Endpoints

### Authentication

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Register a new user |
| `POST` | `/api/v1/auth/login` | Login → returns JWT |
| `POST` | `/api/v1/auth/refresh` | Refresh access token |
| `POST` | `/api/v1/auth/logout` | Invalidate refresh token |

### Transactions

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/transactions` | Create a transaction |
| `GET` | `/api/v1/transactions` | List all transactions |
| `GET` | `/api/v1/transactions/{id}` | Get transaction by ID |
| `DELETE` | `/api/v1/transactions/{id}` | Delete a transaction |

### Budgets

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/budgets` | Create / update budget limit |
| `GET` | `/api/v1/budgets` | List all budgets |

### Admin Operations *(ROLE_ADMIN required)*

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/admin/dlq` | List all DLQ events |
| `POST` | `/api/v1/admin/dlq/{id}/retry` | Retry a DLQ event |
| `DELETE` | `/api/v1/admin/dlq/{id}` | Delete a DLQ event |
| `POST` | `/api/v1/admin/replay` | Replay events from a topic |

### Observability

| Path | Description |
|---|---|
| `GET :8081/actuator/health` | Health check |
| `GET :8081/actuator/metrics` | All registered metrics |
| `GET :8081/actuator/prometheus` | Prometheus scrape endpoint |
| `GET :8081/actuator/info` | Application info |

---

## FlowBridge Event Bus

### Topics

| Topic Constant | Value | Triggered By |
|---|---|---|
| `TRANSACTION_CREATED` | `finance.transaction.created` | New transaction saved |
| `BUDGET_CREATED` | `finance.budget.created` | Budget upserted |
| `BUDGET_EXCEEDED` | `finance.budget.exceeded` | Spending threshold crossed |

### Custom Metrics

| Metric | Tags | Description |
|---|---|---|
| `flowbridge.events.published` | `topic`, `status` | Event publish attempts |
| `flowbridge.events.consumed` | `topic`, `status` | Event consume attempts |
| `flowbridge.dlq.size` | — | Live DLQ depth (Gauge) |

### Event Providers

Configure via `flowbridge.provider`:

| Value | Description |
|---|---|
| `local` | In-memory (default, no external deps) |
| `embedded` | RocksDB-backed persistent store |
| `kafka` | Apache Kafka (requires running broker) |

---

## Observability Stack

```
App (8081) ──/actuator/prometheus──► Prometheus (9090) ──► Grafana (3000)
```

The Grafana dashboard at `monitoring/grafana/dashboards/flowbridge-finance-tracker.json` is auto-provisioned and includes:

- Event publish/consume rates by topic
- DLQ size gauge and trend
- JVM heap memory
- HTTP request p99 latency

---

## Testing

```bash
# Unit tests only
mvn test

# Integration tests (requires PostgreSQL)
mvn verify -pl finance-tracker-tests \
  -DSPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/finance_tracker \
  -DSPRING_DATASOURCE_USERNAME=postgres \
  -DSPRING_DATASOURCE_PASSWORD=postgres
```

---

## Development Phases

| Phase | Description | Status |
|---|---|---|
| 1 | Project Structure, Maven Setup & Architecture | ✅ Done |
| 2 | Authentication & Rate Limiting (JWT + Bucket4j) | ✅ Done |
| 3 | Transaction Management | ✅ Done |
| 4 | Budget Management & Exceed Alerts | ✅ Done |
| 5 | FlowBridge Event Bus Integration | ✅ Done |
| 6 | Admin Replay, DLQ & Retries | ✅ Done |
| 7 | Thymeleaf Admin Dashboard | ✅ Done |
| 8 | Actuator & Observability (Micrometer + Prometheus) | ✅ Done |
| 9 | Dockerization, CI/CD & Documentation | ✅ Done |

---

## License

MIT © FlowBridge Team
