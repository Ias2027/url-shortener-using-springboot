# snip — URL Shortener

A simple URL shortener I built to get hands-on with Spring Boot, REST APIs, and database design.
Paste in a long URL, get back a short one. Clicks are tracked so you can see how often a link gets used.

## How it works

The short codes are created by **Base62-encoding the database row ID**. Since the ID is auto-incremented
and guaranteed unique by MySQL, the short code can never collide with an existing one — no need for
retries or collision checks. The character set is `0-9, a-z, A-Z` (62 chars total), which keeps the
codes short and URL-safe without any special characters.

When someone visits a short link, the app looks up the original URL, bumps the click counter, and
sends back an HTTP 302 redirect. I used 302 (temporary) instead of 301 (permanent) on purpose — a 301
tells the browser to cache the redirect forever, which would mess up click tracking since the browser
would skip the server on repeat visits.

## Tech stack

- **Backend:** Spring Boot 3.3, Java 17
- **Database:** MySQL (with Hibernate/JPA for ORM)
- **Frontend:** Plain HTML/CSS/JS — no framework, just a clean single-page form
- **Containerization:** Dockerfile included (multi-stage build)

## Project structure

```
url-shortener/
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/com/placement/urlshortener/
    │   ├── UrlShortenerApplication.java        # entry point
    │   ├── model/UrlMapping.java               # DB entity
    │   ├── repository/UrlMappingRepository.java
    │   ├── service/UrlShortenerService.java    # core logic (Base62 encoding)
    │   ├── controller/
    │   │   ├── UrlApiController.java           # REST API endpoints
    │   │   └── UrlRedirectController.java      # handles /r/{code} redirects
    │   ├── dto/                                # request/response objects
    │   ├── exception/                          # error handling
    │   └── config/WebConfig.java               # CORS config
    └── resources/
        ├── application.properties
        └── static/                             # frontend files
```

## Running locally

You'll need Java 17+, Maven, and MySQL installed.

1. Create the database (or just let it auto-create — `createDatabaseIfNotExist=true` is already set):
   ```sql
   CREATE DATABASE IF NOT EXISTS url_shortener;
   ```
2. Check `src/main/resources/application.properties` — the defaults are `root`/`root` for MySQL.
   Update them if yours are different.
3. Start the app:
   ```bash
   mvn spring-boot:run
   ```
4. Open [http://localhost:8080](http://localhost:8080) — the frontend loads automatically.

## API

| Method | Endpoint | What it does |
|--------|----------|--------------|
| POST | `/api/urls/shorten` | Shortens a URL. Body: `{"url": "https://...", "expiresInMinutes": 60}` (expiry is optional) |
| GET | `/api/urls/{code}/stats` | Returns click count, creation date, expiry |
| GET | `/r/{code}` | Redirects to the original URL and increments click count |

### Example

```bash
# Shorten a URL
curl -X POST http://localhost:8080/api/urls/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://docs.spring.io/spring-boot/docs/current/reference/html/"}'

# Check stats
curl http://localhost:8080/api/urls/1c/stats
```

## Deploying (Docker)

The repo includes a multi-stage Dockerfile. To deploy on something like Render or Railway:

1. Push to GitHub
2. Connect the repo on your cloud platform — it'll detect the Dockerfile
3. Set these environment variables for your managed MySQL:
   - `DB_URL` — e.g. `jdbc:mysql://host:3306/dbname`
   - `DB_USERNAME`
   - `DB_PASSWORD`

The app already reads from environment variables with local fallbacks, so the same code works
in both dev and production without any changes.

## Design decisions worth noting

- **Base62 over random strings:** No collision handling needed since database IDs are unique by definition.
  The trade-off is that codes are sequential (id 100 produces a similar code to id 101), which means
  someone could guess other short URLs. For a production system you'd want to shuffle the mapping.
- **DTOs instead of exposing entities:** The API response shape is decoupled from the database schema,
  so either can change independently.
- **Global exception handler:** Errors are caught centrally (`@RestControllerAdvice`) instead of
  try-catch blocks in every controller — keeps things cleaner.
- **302 redirect for click tracking:** Already covered above, but it's an intentional choice.

## What I'd add with more time

- Redis caching for frequently accessed short codes
- Rate limiting on the shorten endpoint
- A dashboard page showing all created links and their stats
- Proper unit and integration tests
