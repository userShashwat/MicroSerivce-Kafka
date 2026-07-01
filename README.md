# Distributed Notification Service

A production-grade, event-driven notification system built with Spring Boot microservices and Apache Kafka. Three independent services communicate asynchronously via Kafka topics — User Service and Order Service publish events, Notification Service consumes them and dispatches real emails via JavaMail with Freemarker templates.

---

## Architecture

```
┌─────────────────┐        ┌─────────────────┐
│  User Service    │        │  Order Service   │
│  Port: 8081       │        │  Port: 8082       │
│                   │        │                   │
│  • Register       │        │  • Place Order     │
│  • Login (JWT)     │        │  • Cancel Order     │
│  • Preferences     │        │  • Get Orders       │
└────────┬──────────┘        └────────┬──────────┘
         │                            │
         │ users.events               │ order.placed
         │                            │ order.cancelled
         ▼                            ▼
┌─────────────────────────────────────────────┐
│              Apache Kafka (KRaft)             │
│                                                │
│  Topics:                                      │
│   • users.events      (UserRegisteredEvent)   │
│   • order.placed      (OrderPlacedEvent)      │
│   • order.cancelled   (OrderCancelledEvent)   │
└─────────────────────┬─────────────────────────┘
                       │
                       ▼
          ┌────────────────────────┐
          │  Notification Service    │
          │  Port: 8083                │
          │                            │
          │  • Kafka Consumers          │
          │  • Event Router             │
          │  • JavaMail + Freemarker     │
          │  • Resilience4j Retry        │
          │  • Circuit Breaker            │
          │  • Dead Letter Queue           │
          │  • Spring Actuator               │
          │  • Notification History           │
          └──────────┬──────────────────────┘
                      │
           ┌──────────┴──────────┐
           │                     │
           ▼                     ▼
     ┌──────────┐         ┌──────────────┐
     │  MySQL     │         │  Mailtrap      │
     │            │         │  SMTP           │
     │ user_db     │         │ (sandbox)        │
     │ order_db     │         └──────────────┘
     │ notif_db      │
     └──────────┘
```

---

## Services

### User Service (Port 8081)

Handles user registration and authentication. Publishes a `UserRegisteredEvent` to Kafka on every successful registration.

| Endpoint | Method | Description | Auth |
|---|---|---|---|
| `/api/auth/register` | POST | Register new user | Public |
| `/api/auth/login` | POST | Login and get JWT token | Public |

### Order Service (Port 8082)

Manages orders. Publishes `OrderPlacedEvent` and `OrderCancelledEvent` to separate Kafka topics.

| Endpoint | Method | Description | Auth |
|---|---|---|---|
| `/api/orders` | POST | Place a new order | Public |
| `/api/orders/{id}/cancel` | PATCH | Cancel an order | Public |
| `/api/orders/user/{userId}` | GET | Get orders by user | Public |

### Notification Service (Port 8083)

The core service. Consumes Kafka events, dispatches emails, and persists notification history.

| Endpoint | Method | Description |
|---|---|---|
| `/api/notifications/user/{userId}` | GET | Get notification history |
| `/api/notifications/user/{userId}/unread-count` | GET | Get unread count |
| `/actuator/health` | GET | Health check (DB, Mail, Kafka) |
| `/actuator/metrics` | GET | Service metrics |

---

## Kafka Topics

| Topic | Producer | Event | Consumer |
|---|---|---|---|
| `users.events` | User Service | `UserRegisteredEvent` | Notification Service |
| `order.placed` | Order Service | `OrderPlacedEvent` | Notification Service |
| `order.cancelled` | Order Service | `OrderCancelledEvent` | Notification Service |

> **Why separate topics for order events?** Using a single `order.events` topic with multiple consumer groups caused both consumer groups to receive every message — `OrderPlacedEvent` was being deserialized as `OrderCancelledEvent` by the wrong consumer. Separate topics eliminate this entirely.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.5 |
| Security | Spring Security, JWT (jjwt 0.11.5) |
| Messaging | Apache Kafka (KRaft mode — no Zookeeper) |
| ORM | Spring Data JPA, Hibernate |
| Database | MySQL 8.0 |
| Email | JavaMail (SMTP) + Freemarker templates |
| Resilience | Resilience4j — retry, circuit breaker, exponential backoff |
| Observability | Spring Boot Actuator |
| Containerisation | Docker, Docker Compose (multi-stage builds) |
| CI/CD | GitHub Actions → AWS EC2 |

---

## Resilience Pattern

```
Email dispatch attempt
        │
        ▼
┌───────────────────┐
│  Circuit Breaker    │ ← Opens after 50% failure rate over 5 requests
│  (Resilience4j)      │   Waits 10s before retrying
└────────┬────────────┘
         │ CLOSED (normal)
         ▼
┌───────────────────┐
│  Retry (max 3)       │ ← Exponential backoff: 2s → 4s → 8s
│  Resilience4j          │
└────────┬────────────┘
         │ All retries failed
         ▼
┌───────────────────┐
│  Dead Letter          │ ← Event published to notifications.dlq
│  Topic (DLQ)            │   Status updated to FAILED in MySQL
└───────────────────┘
```

---

## Database Schema

### `notification_db` — `notifications` table

| Column | Type | Description |
|---|---|---|
| `id` | BIGINT PK | Auto-increment |
| `user_id` | BIGINT | References user |
| `email` | VARCHAR | Recipient email |
| `notification_type` | ENUM | `USER_REGISTERED`, `ORDER_PLACED`, `ORDER_CANCELLED` |
| `status` | ENUM | `PENDING`, `SENT`, `FAILED`, `SKIPPED` |
| `payload` | TEXT (JSON) | Full event payload stored for debugging |
| `retry_count` | INT | 0 to 3 — incremented on each retry |
| `created_at` | TIMESTAMP | When event was received |
| `sent_at` | TIMESTAMP | When notification was dispatched |

---

## Local Setup

### Prerequisites

- Docker Desktop running
- Java 21
- Maven 3.9+

### 1. Clone the repo

```bash
git clone https://github.com/userShashwat/MicroSerivce-Kafka.git
cd MicroSerivce-Kafka
```

### 2. Set up Mailtrap (free SMTP sandbox)

Sign up at [mailtrap.io](https://mailtrap.io/) → Email Testing → Inboxes → SMTP Settings → copy your username and password.

### 3. Update environment variables

In `docker-compose.yml`, set your credentials:

```yaml
notification-service:
  environment:
    SPRING_MAIL_USERNAME: your_mailtrap_username
    SPRING_MAIL_PASSWORD: your_mailtrap_password
    MYSQL_ROOT_PASSWORD: your_mysql_password
```

### 4. Start all services

```bash
docker-compose up --build -d
```

This starts 5 containers:

- `kafka` — Apache Kafka (KRaft, port 9092)
- `mysql` — MySQL 8.0 (port 3307)
- `user-service` — port 8081
- `order-service` — port 8082
- `notification-service` — port 8083

### 5. Verify everything is running

```bash
docker ps
```

All 5 containers should show `Up` and `Healthy`.

---

## Testing the Full Flow

### Register a user

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name": "Shashwat", "email": "test@gmail.com", "password": "password123"}'
```

### Place an order

```bash
curl -X POST http://localhost:8082/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "product": "MacBook Pro", "quantity": 1, "price": 150000}'
```

### Cancel an order

```bash
curl -X PATCH http://localhost:8082/api/orders/1/cancel
```

### Check notification history

```bash
curl http://localhost:8083/api/notifications/user/1
```

### Check health

```bash
curl http://localhost:8083/actuator/health
```

Expected response:

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "mail": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

---

## CI/CD Pipeline

Every push to `main` triggers the GitHub Actions pipeline:

```
Push to main
     │
     ▼
Build JAR (mvn package)              ← runs on GitHub Actions runner
     │
     ▼
Copy JARs + Dockerfiles to EC2       ← SCP to EC2
     │
     ▼
docker-compose up --build            ← SSH into EC2, restart all containers
     │
     ▼
All 5 containers live on AWS EC2     ← zero manual steps
```

---

## Design Decisions

**Why Kafka instead of REST calls between services?**
REST calls create tight coupling — if Notification Service is down, User Service registration fails. With Kafka, User Service publishes the event and forgets. Notification Service picks it up whenever it's ready. No data loss, no coupling.

**Why separate Kafka topics for order events?**
With a single `order.events` topic and multiple consumer groups, both consumer groups received every message — causing `OrderPlacedEvent` to be misread as `OrderCancelledEvent`. Separate `order.placed` and `order.cancelled` topics eliminate this entirely.

**Why KRaft instead of Zookeeper?**
Zookeeper is deprecated in newer Kafka versions. KRaft mode allows Kafka to manage its own metadata without an external coordinator — simpler setup, fewer containers.

**Why Resilience4j over manual retry?**
Manual retry logic is error-prone and hard to test. Resilience4j provides annotation-based retry and circuit breaker with exponential backoff — declarative, testable, and production-standard.

---

## Project Structure

```
MicroSerivce-Kafka/
├── User/Service/
│   └── src/main/java/com/user/Service/
│       ├── controller/   AuthController.java
│       ├── dto/          RegisterRequest, LoginRequest, AuthResponse
│       ├── entity/       User.java
│       ├── event/        UserRegisteredEvent.java
│       ├── kafka/        UserEventProducer.java
│       ├── repository/   UserRepository.java
│       ├── security/     JwtUtil, JwtAuthFilter, SecurityConfig
│       └── service/      UserService.java
├── Order/Service/
│   └── src/main/java/com/order/Service/
│       ├── controller/   OrderController.java
│       ├── dto/          OrderRequest.java
│       ├── entity/       Order.java
│       ├── event/        OrderPlaceEvent, OrderCancelledEvent
│       ├── kafka/        OrderEventProducer.java
│       ├── repository/   OrderRepository.java
│       └── service/      OrderService.java
├── Notification/Service/
│   └── src/main/java/com/notification/Service/
│       ├── config/       KafkaConsumerConfig.java
│       ├── controller/   NotificationController.java
│       ├── entity/       Notification.java
│       ├── event/        UserRegisteredEvent, OrderPlacedEvent, OrderCancelledEvent
│       ├── kafka/        NotificationConsumer.java
│       ├── repository/   NotificationRepository.java
│       └── service/      EmailService, NotificationService
├── docker-compose.yml
└── init.sql
```

---

## Author

**Shashwat Sharma**
B.Tech CSE — KIIT University, Bhubaneswar (2023–2027)
