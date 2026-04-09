# API Gateway (legacy folder)

> **Active gateway:** [`services/gateway-service`](../services/gateway-service) — Spring Cloud Gateway. `docker-compose.yml` builds that image; this folder is not used by Compose.

## Overview

The API Gateway serves as the single entry point for all client requests. It routes incoming requests to the appropriate backend microservice.

## Responsibilities

- **Request routing**: Forward requests to the correct service
- **Load balancing**: Distribute traffic (if applicable)
- **Authentication**: Validate tokens/credentials (optional)
- **Rate limiting**: Protect services from overload (optional)
- **CORS handling**: Allow frontend cross-origin requests
- **Request/Response transformation**: Modify headers, paths as needed

## Tech Stack

| Component  | Choice             |
|------------|--------------------|
| Approach   | Spring Cloud Gateway (see `services/gateway-service`) |

## Routing Table

| External Path        | Target Service | Internal URL                              |
|----------------------|----------------|-------------------------------------------|
| `/menu/**`           | Menu Service   | `http://menu-service:5000` (strip `/menu`) |
| `/orders/**`         | Order Service  | `http://order-service:5000`               |
| `/delivery/**`       | Delivery & Payment | `http://delivery-payment-service:5000` (strip `/delivery`) |

## Running

```bash
# From project root
docker compose up gateway-service --build
```

## Configuration

The gateway uses Docker Compose networking. Services are accessible by their
service names defined in `docker-compose.yml` (e.g., `menu-service`, `order-service`, `delivery-payment-service`).

## Notes

- Use service names (not `localhost`) for upstream URLs inside Docker
- The gateway exposes port 8080 to the host
