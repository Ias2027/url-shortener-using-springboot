package com.placement.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. Spring Boot auto-configures the embedded Tomcat server,
 * the database connection, and component scanning from this package down.
 *
 * Run this class (or `mvn spring-boot:run`) to start the app on port 8080.
 */
@SpringBootApplication
public class UrlShortenerApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
