# snip — URL shortener

A URL shortener built with Spring Boot 3, Java 17, and vanilla JavaScript. It creates Base62 short codes, tracks redirects, and supports expiring links.

## Quick start

Install Java 17 and Maven, then run:

```bash
cd url-shortener
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). Local development uses a persistent H2 file, so MySQL is not required. See [url-shortener/README.md](url-shortener/README.md) for MySQL Docker setup, configuration, and API details.
