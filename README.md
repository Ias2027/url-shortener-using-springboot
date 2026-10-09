# URL Shortener — Spring Boot

A URL shortener built with Spring Boot, MySQL, and vanilla JavaScript.
Converts long URLs into short, unique codes using Base62 encoding of database IDs — no collisions by design.
Includes click tracking and optional link expiration.

→ See [url-shortener/README.md](url-shortener/README.md) for full setup instructions, API docs, and design notes.

## Quick start

```bash
# Make sure MySQL is running, then:
cd url-shortener
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) and start shortening URLs.
