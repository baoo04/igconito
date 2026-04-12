# Movie Service

Catalog service: movies, cinemas, auditoriums, and showtimes. Persists to `movie_db` (MySQL).

## Run locally (requires MySQL)

```bash
export DB_HOST=localhost DB_PORT=3306 DB_NAME=movie_db DB_USER=root DB_PASSWORD=root
mvn spring-boot:run
```

## Docker

Built from this directory via root `docker compose build movie-service`. Listens on port **5000** inside the container; compose maps **5001** by default.

## API

- `GET /health` — `{"status":"ok","service":"movie-service"}`
- `GET /swagger-ui.html` — Swagger UI
- REST resources: `/movies`, `/cinemas`, `/showtimes` (see `docs/api-specs/movie-service.yaml`)

## Inter-service URL (Docker network)

`http://movie-service:5000`
