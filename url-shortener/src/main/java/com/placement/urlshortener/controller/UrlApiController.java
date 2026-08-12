package com.placement.urlshortener.controller;

import com.placement.urlshortener.dto.ShortenRequest;
import com.placement.urlshortener.dto.ShortenResponse;
import com.placement.urlshortener.model.UrlMapping;
import com.placement.urlshortener.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST endpoints under /api/urls. Kept separate from the redirect
 * controller (UrlRedirectController) because they serve different
 * purposes: this one is the JSON API the frontend calls, the other
 * handles actual browser redirects for anyone who clicks a short link.
 */
@RestController
@RequestMapping("/api/urls")
public class UrlApiController {

    private final UrlShortenerService service;

    public UrlApiController(UrlShortenerService service) {
        this.service = service;
    }

    // POST /api/urls/shorten  { "url": "https://...", "expiresInMinutes": 60 }
    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request,
                                                     HttpServletRequest httpRequest) {
        LocalDateTime expiresAt = request.getExpiresInMinutes() != null
                ? LocalDateTime.now().plusMinutes(request.getExpiresInMinutes())
                : null;

        UrlMapping mapping = service.createShortUrl(request.getUrl(), expiresAt);
        return ResponseEntity.ok(new ShortenResponse(mapping, baseUrl(httpRequest)));
    }

    // GET /api/urls/{code}/stats  -> click count, created date, etc.
    @GetMapping("/{code}/stats")
    public ResponseEntity<ShortenResponse> stats(@PathVariable String code,
                                                   HttpServletRequest httpRequest) {
        UrlMapping mapping = service.getStats(code);
        return ResponseEntity.ok(new ShortenResponse(mapping, baseUrl(httpRequest)));
    }

    private String baseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443
                    ? "" : ":" + request.getServerPort());
    }
}
