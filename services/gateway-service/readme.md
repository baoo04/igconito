# Gateway Service (Spring Cloud Gateway)

## Overview

Single entry point for the Food Ordering system. Routes `/menu/**`, `/orders/**`, and `/delivery/**` to backend services and exposes `GET /health`.

## Tech Stack

| Component | Choice |
|-----------|--------|
| Runtime | Java 21 |
| Framework | Spring Boot 3.4 + Spring Cloud Gateway |

## Routes

| External prefix | Target |
|-----------------|--------|
| `/menu/**` | menu-service:5000 (prefix stripped) |
| `/orders/**` | order-service:5000 |
| `/delivery/**` | delivery-payment-service:5000 (prefix stripped) |

## Running

```bash
docker compose up gateway-service --build
```

## Environment

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Listen port | `8080` |
