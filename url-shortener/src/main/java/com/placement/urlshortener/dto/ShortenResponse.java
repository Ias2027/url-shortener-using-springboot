package com.placement.urlshortener.dto;

import com.placement.urlshortener.model.UrlMapping;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ShortenResponse {
    private final String shortCode;
    private final String shortUrl;
    private final String originalUrl;
    private final long clickCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;

    public ShortenResponse(UrlMapping mapping, String baseUrl) {
        this.shortCode = mapping.getShortCode();
        this.shortUrl = baseUrl + "/r/" + mapping.getShortCode();
        this.originalUrl = mapping.getOriginalUrl();
        this.clickCount = mapping.getClickCount();
        this.createdAt = mapping.getCreatedAt();
        this.expiresAt = mapping.getExpiresAt();
    }
}
