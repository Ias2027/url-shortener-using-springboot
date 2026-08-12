package com.placement.urlshortener.controller;

import com.placement.urlshortener.model.UrlMapping;
import com.placement.urlshortener.service.UrlShortenerService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Handles the actual redirect when someone visits a short link, e.g.
 * https://yourapp.com/r/aZ3fT9
 *
 * Uses HTTP 302 (Found / temporary redirect), not 301 (Moved Permanently).
 * This is a deliberate choice worth explaining if asked:
 *   - 301 tells browsers "cache this forever" - after the first click,
 *     the browser may skip calling your server entirely on future clicks
 *     and go straight to the original URL. That breaks click tracking.
 *   - 302 says "this redirect might change" - the browser asks your
 *     server every time, so clickCount stays accurate.
 * Trade-off: 302 is marginally slower (no browser-side caching) but
 * correctness of analytics wins here.
 */
@RestController
public class UrlRedirectController {

    private final UrlShortenerService service;

    public UrlRedirectController(UrlShortenerService service) {
        this.service = service;
    }

    @GetMapping("/r/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        UrlMapping mapping = service.resolveAndTrackClick(code);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(mapping.getOriginalUrl()))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .build();
    }
}
