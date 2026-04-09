# Menu Service

## Overview

Describe the responsibility of this service:
- What business domain does it cover?
- What data does it own?
- What operations does it expose?

This service covers the **Menu Catalog** bounded context:
- Owns **categories**, **food items**, **combos/bundles**, and **reviews**
- Acts as the **source of truth for pricing** (dynamic price quote endpoints)

## Tech Stack

| Component  | Choice |
|------------|--------|
| Language   | Java 21 |
| Framework  | Spring Boot 3.4 |
| Database   | MySQL (`menu_service_db`) |

## API Endpoints

| Method | Endpoint      | Description          |
|--------|---------------|----------------------|
| GET    | `/health`     | Health check         |
| GET    | `/categories` | List categories       |
| POST   | `/categories` | Create category       |
| GET    | `/foods`      | Search/list foods     |
| POST   | `/foods`      | Create food           |
| GET    | `/foods/{id}` | Food detail (+ review stats) |
| GET    | `/foods/{id}/price?size=L` | Price quote (dynamic) |
| GET    | `/foods/{id}/reviews` | List reviews         |
| POST   | `/foods/{id}/review`  | Add review           |
| GET    | `/combos`     | List combos           |
| POST   | `/combos`     | Create combo          |
| GET    | `/combos/{id}/price` | Combo price quote (dynamic) |

> Full API specification: [`docs/api-specs/menu-service.yaml`](../../docs/api-specs/menu-service.yaml)

## Running Locally

```bash
# From project root (MySQL + this service)
docker compose up mysql menu-service --build

# Or Maven on the host (MySQL must be reachable at MYSQL_HOST)
cd services/menu-service
mvn spring-boot:run
```

## Project Structure

```
menu-service/
├── Dockerfile
├── pom.xml
├── readme.md
└── src/main/java/com/foodorder/menuservice/
    └── controller/
```

## Environment Variables

| Variable          | Description              | Default (local / Compose) |
|-------------------|--------------------------|---------------------------|
| `SERVER_PORT`     | HTTP port inside JVM     | `5000`                    |
| `MYSQL_HOST`      | MySQL hostname           | `localhost` / `mysql`     |
| `MYSQL_PORT`      | MySQL port               | `3306`                    |
| `MYSQL_DATABASE`  | Schema name              | `menu_service_db`         |
| `MYSQL_USER`      | DB user                  | `foodorder`               |
| `MYSQL_PASSWORD`  | DB password              | `foodorder`               |

### Pricing (dynamic, demo)

| Variable | Description | Default |
|---|---|---|
| `MENU_PRICING_ZONE_ID` | Zone for happy-hour evaluation | `Asia/Ho_Chi_Minh` |
| `MENU_HAPPY_HOUR_START` | Start (HH:mm, inclusive) | `18:00` |
| `MENU_HAPPY_HOUR_END` | End (HH:mm, exclusive) | `20:00` |
| `MENU_HAPPY_HOUR_DISCOUNT_PERCENT` | Discount percent | `10` |

> **MySQL Workbench (Docker MySQL):** Host **`127.0.0.1`**, port **`MYSQL_HOST_PORT`** (`.env`). Nên dùng **`foodorder` / `foodorder`** — user có `@'%'` nên không lỗi `1045` khi client hiện là `172.18.0.1` (Docker Desktop). **`root`**: mật khẩu **`MYSQL_ROOT_PASSWORD`**; volume mới chạy `scripts/mysql-init/02-grant-root-remote.sh` để có `root@'%'`. Volume cũ: mở terminal, `docker compose exec mysql mysql -uroot -p` (nhập mật khẩu root), rồi chạy SQL tạo `root@'%'` giống trong file `02-grant-root-remote.sh`, hoặc chỉ dùng **foodorder**.
