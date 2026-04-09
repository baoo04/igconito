# Delivery & Payment Service

## Overview

Describe the responsibility of this service:
- What business domain does it cover?
- What data does it own?
- What operations does it expose?

## Tech Stack

| Component  | Choice |
|------------|--------|
| Language   | Java 21 |
| Framework  | Spring Boot 3.4 |
| Database   | MySQL (`delivery_payment_service_db`) |

## API Endpoints

| Method | Endpoint      | Description          |
|--------|---------------|----------------------|
| GET    | `/health`     | Health check         |
| ...    | ...           | ...                  |

> Full API specification: [`docs/api-specs/delivery-payment-service.yaml`](../../docs/api-specs/delivery-payment-service.yaml)

## Running Locally

```bash
# From project root
docker compose up mysql delivery-payment-service --build

cd services/delivery-payment-service
mvn spring-boot:run
```

## Project Structure

```
delivery-payment-service/
├── Dockerfile
├── pom.xml
├── readme.md
└── src/main/java/com/foodorder/deliverypayment/
    └── controller/
```

## Environment Variables

| Variable          | Description              | Default (local / Compose) |
|-------------------|--------------------------|---------------------------|
| `SERVER_PORT`     | HTTP port inside JVM     | `5000`                    |
| `MYSQL_HOST`      | MySQL hostname           | `localhost` / `mysql`     |
| `MYSQL_PORT`      | MySQL port               | `3306`                    |
| `MYSQL_DATABASE`  | Schema name              | `delivery_payment_service_db` |
| `MYSQL_USER`      | DB user                  | `foodorder`               |
| `MYSQL_PASSWORD`  | DB password              | `foodorder`               |
