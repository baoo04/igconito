# Frontend

## Overview

This is the frontend module of the microservices system. It provides the user interface and communicates with backend services through the API Gateway.

## Tech Stack

| Component        | Choice               |
|------------------|----------------------|
| Framework        | React 18 |
| Styling          | Inline / minimal CSS |
| Package Manager  | npm |
| Build Tool       | Vite 6 |

## Getting Started

```bash
# From project root
docker compose up frontend --build

# Or run locally (adapt to your stack)
cd src/
# npm install && npm run dev
# yarn && yarn dev
```

## Project Structure

```
frontend/
├── Dockerfile
├── readme.md
└── src/           # Your source code goes here
```

## Environment Variables

| Variable       | Description                | Default                  |
|----------------|----------------------------|--------------------------|
| `API_BASE_URL` | URL of the API Gateway     | `http://localhost:8080`  |

## Build for Production

```bash
# Example:
# npm run build
# yarn build
```

## Notes

- All API calls should go through the **API Gateway** (`gateway-service`), not directly to individual services.
- Configure proxy or API base URL to point to the gateway.
- Backend routes (via gateway): `/menu/`, `/orders/`, `/delivery/` (see [`services/gateway-service/readme.md`](../services/gateway-service/readme.md)).
- UI flow: **Thực đơn** → đặt hàng (`POST /orders`) → **Đơn hàng của tôi** → mở rộng đơn → thanh toán mock (`POST /delivery/payments/mock`), giao hàng (`POST /delivery/deliveries`), cập nhật trạng thái (`PATCH` delivery / order).