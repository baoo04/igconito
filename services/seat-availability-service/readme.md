# Seat Availability Service

Manages per-showtime seat layout, temporary holds (TTL configurable, default 5 minutes), and transitions to booked seats. Uses `seat_db` (MySQL).

On first access for a showtime, a default grid is created (rows A–H, 12 seats per row by default) after validating the showtime exists in **movie-service** (`GET /showtimes/{id}`). Code paths that may insert seats must **not** use `@Transactional(readOnly = true)` (otherwise JDBC rejects writes and the API can return 500).

## Run locally

Requires MySQL with `seat_db` and a running movie-service for showtime validation.

```bash
set DB_HOST=localhost& set DB_PORT=3306& set DB_NAME=seat_db& set DB_USER=root& set DB_PASSWORD=root
set MOVIE_SERVICE_BASE_URL=http://localhost:5001
```

`MOVIE_SERVICE_BASE_URL` must point at movie-service’s base URL (including host port when running outside Docker).

## Docker

Compose service name: `seat-availability-service`, port **5002** → **5000** in container.

## API (internal)

- `GET /health`
- `GET /showtimes/{showtimeId}/seats`
- `POST /showtimes/{showtimeId}/availability` — body `{ "seatIds": [1,2] }`
- `POST /holds` — `{ "showtimeId", "seatIds" }`
- `GET /holds/{holdId}`
- `POST /holds/{holdId}/release`
- `POST /holds/{holdId}/confirm`

See `docs/api-specs/seat-availability-service.yaml`.
