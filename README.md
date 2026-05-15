# Payment Service — Local Setup Guide

Spring Boot 3.x · Java 17 · Kafka · H2 · Docker · Kubernetes · Jenkins

---

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Java (JDK) | 17+ | https://adoptium.net |
| Maven | 3.9+ | https://maven.apache.org |
| Docker Desktop | latest | https://docker.com/products/docker-desktop |
| Git | any | https://git-scm.com |

---

## Project Structure

```
payment-app/
├── src/
│   ├── main/java/com/payment/
│   │   ├── PaymentServiceApplication.java   ← Main class
│   │   ├── controller/PaymentController.java
│   │   ├── service/PaymentService.java
│   │   ├── model/Payment.java               ← JPA entity
│   │   ├── model/PaymentDTO.java            ← Request/Response DTOs
│   │   ├── repository/PaymentRepository.java
│   │   ├── kafka/PaymentProducer.java
│   │   ├── kafka/PaymentConsumer.java
│   │   └── config/
│   │       ├── KafkaTopicConfig.java
│   │       └── GlobalExceptionHandler.java
│   ├── main/resources/
│   │   └── application.yml                 ← App config (H2 + Kafka)
│   └── test/java/com/payment/
│       └── PaymentServiceIntegrationTest.java
├── docker/
│   └── docker-compose.yml                  ← Kafka + Zookeeper + Kafka-UI
├── k8s/
│   └── deployment.yaml                     ← K8s Deployment + Service + HPA
├── jenkins/
│   └── Jenkinsfile                         ← CI/CD pipeline
├── Dockerfile                              ← Multi-stage Docker build
└── pom.xml
```

---

## Step 1 — Start Kafka Locally (Docker)

```bash
cd docker
docker-compose up -d
```

Verify it's running:
```bash
docker-compose ps
# zookeeper   Up   2181
# kafka       Up   9092
# kafka-ui    Up   8090
```

**Kafka UI** → open http://localhost:8090 in your browser to watch messages in real time.

---

## Step 2 — Run the Spring Boot App

```bash
# From the project root
mvn spring-boot:run
```

The app starts on **http://localhost:8080**

**H2 Console** → http://localhost:8080/h2-console  
- JDBC URL: `jdbc:h2:mem:paymentdb`  
- Username: `sa` / Password: *(blank)*

**Health Check** → http://localhost:8080/actuator/health

---

## Step 3 — Test the API

### Create an ISO 20022 Payment (SEPA)
```bash
curl -s -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccount": "DE89370400440532013000",
    "toAccount":   "GB29NWBK60161331926819",
    "amount":      1500.00,
    "currency":    "EUR",
    "standard":    "ISO_20022",
    "description": "Invoice payment Q2"
  }' | jq .
```

### Create an ISO 8583 Payment (Card/POS)
```bash
curl -s -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccount": "4111111111111111",
    "toAccount":   "MERCHANT-001",
    "amount":      49.99,
    "currency":    "USD",
    "standard":    "ISO_8583",
    "description": "POS transaction"
  }' | jq .
```

### Process a Payment (takes it from PENDING → COMPLETED)
```bash
# Replace {id} with the id from the create response
curl -s -X POST http://localhost:8080/api/v1/payments/{id}/process | jq .
```

### Get All Payments
```bash
curl -s http://localhost:8080/api/v1/payments | jq .
```

### Get by ID
```bash
curl -s http://localhost:8080/api/v1/payments/{id} | jq .
```

### Reverse a Completed Payment
```bash
curl -s -X POST http://localhost:8080/api/v1/payments/{id}/reverse | jq .
```

---

## Step 4 — Run Tests

```bash
mvn test
```

Tests use **embedded Kafka** — no external dependencies needed.

```
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
```

---

## Step 5 — Build Docker Image (Optional)

```bash
# Build
docker build -t payment-service:1.0.0 .

# Run (connects to local Kafka via host.docker.internal)
docker run -p 8080:8080 \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  payment-service:1.0.0
```

---

## API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/payments` | Create a new payment |
| GET | `/api/v1/payments` | List all payments |
| GET | `/api/v1/payments/{id}` | Get payment by ID |
| GET | `/api/v1/payments/account/{acc}` | Get by account |
| POST | `/api/v1/payments/{id}/process` | Process (PENDING → COMPLETED) |
| POST | `/api/v1/payments/{id}/reverse` | Reverse (COMPLETED → REVERSED) |
| GET | `/actuator/health` | Health check |

---

## Payment Standards

| Standard | Used For | Reference ID prefix |
|----------|----------|---------------------|
| ISO_20022 | SEPA transfers, bank wires | `SEPA-XXXXXXXX` |
| ISO_8583 | Card/POS transactions | `POS-XXXXXXXX` |

---

## Kafka Topics

| Topic | Published when |
|-------|----------------|
| `payment.initiated` | Payment created |
| `payment.processed` | Payment completed |
| `payment.failed` | Payment reversed/failed |

---

## Stopping Everything

```bash
# Stop Kafka
cd docker && docker-compose down

# Stop Spring Boot
Ctrl+C in terminal
```
