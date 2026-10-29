# snip — URL shortener

A URL shortener built with Spring Boot 3 and Java 17. It generates Base62 short codes, tracks redirects, and supports expiring links.

## Run locally

Install Java 17 and Maven, then run:

```bash
cd url-shortener
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). Local development uses a persistent H2 database file, so MySQL is not required. Maven creates the database automatically on first launch.

## Run with MySQL in Docker

Install Docker Compose, copy `.env.example` to `.env`, and set a local database password in `.env`. Then run from the repository root:

```bash
docker compose up --build
```

Open [http://localhost:8080](http://localhost:8080). The MySQL data is stored in the `mysql-data` Docker volume.

## API

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/urls/shorten` | Create a short link. Body: `{"url":"https://example.com","expiresInMinutes":60}`; expiry is optional. |
| `GET` | `/api/urls/{code}/stats` | Read link stats, including click count and expiry. |
| `GET` | `/r/{code}` | Record a click and redirect to the original URL. |

Example:

```bash
curl -X POST http://localhost:8080/api/urls/shorten \
  -H "Content-Type: application/json" \
  -d '{"url":"https://spring.io"}'
```

The create endpoint returns `201 Created`. Invalid URLs and invalid expiry values return `400`; unknown and expired short codes return `404`.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `PORT` | `8080` | HTTP port |
| `DB_URL` | Persistent local H2 file | JDBC URL; set a MySQL JDBC URL to use MySQL |
| `DB_USERNAME` | `sa` | Database username |
| `DB_PASSWORD` | Empty | Database password |

For a hosted MySQL service, set all three `DB_*` variables. Keep credentials in the host's secret store; do not commit `.env` or passwords.

## Design notes

- Base62 encoding of the database ID guarantees unique, compact short codes. Codes are sequential and guessable, so use random codes if links need to resist enumeration.
- Each request creates its own link. This keeps requested expiry behavior predictable when the same destination is shortened more than once.
- Redirect clicks use an atomic database increment so concurrent visits are counted correctly.
- H2 is intended for a simple local run. Use MySQL for a multi-instance or production deployment, and replace schema auto-update with a migration tool before treating the service as production infrastructure.
