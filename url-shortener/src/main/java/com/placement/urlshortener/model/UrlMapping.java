package com.placement.urlshortener.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One row = one shortened URL.
 *
 * id            -> auto-increment primary key. This is the number we encode
 *                  into Base62 to produce the short code (see UrlShortenerService).
 * shortCode     -> the human-facing short string, e.g. "aZ3fT9". Indexed + unique
 *                  because every redirect lookup happens by this column.
 * originalUrl   -> the long URL the user submitted.
 * clickCount    -> incremented every time someone visits the short link.
 * createdAt     -> for showing "created X ago" and for expiry logic if you add it later.
 * expiresAt     -> optional. Null = never expires.
 */
@Entity
@Table(name = "url_mapping", indexes = {
        @Index(name = "idx_short_code", columnList = "shortCode", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String shortCode;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable = false)
    private long clickCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime expiresAt; // nullable -> no expiry
}
