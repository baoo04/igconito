# API Gateway

Spring Cloud Gateway (Spring Boot **3.4** + Spring Cloud **2024.0**) listening on **8080**. Dùng cặp này để tránh lệch `spring-web` / Gateway (lỗi `HttpHeaders.headerSet` NoSuchMethodError → 502).

## Routes (order matters)

| Path pattern | Target |
|--------------|--------|
| `/showtimes/*/seats`, `/showtimes/*/availability` | `seat-availability-service:5000` |
| `/holds/**` | `seat-availability-service:5000` |
| `/bookings/**` | `ticket-booking-service:5000` |
| `/customers/**` | `customer-service:5000` |
| `/payments/**` | `payment-service:5000` |
| `/notifications/**` | `notification-service:5000` |
| `/movies/**`, `/cinemas/**`, `/showtimes/**` | `movie-service:5000` |

Seat-specific `/showtimes/...` routes are registered before the movie catch-all.

## Health

- `GET http://localhost:8080/actuator/health`

## Configuration

Set `MOVIE_SERVICE_HOST` (default `movie-service`) to point at the catalog service on the Docker network.
