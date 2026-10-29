package com.placement.urlshortener.service;

import com.placement.urlshortener.exception.ShortCodeNotFoundException;
import com.placement.urlshortener.model.UrlMapping;
import com.placement.urlshortener.repository.UrlMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ============================================================
 * THE CORE ALGORITHM - know this section cold for interviews
 * ============================================================
 *
 * Approach used here: Base62 encoding of the auto-increment DB id.
 *
 * WHY THIS APPROACH (vs alternatives):
 *   1) Random string + collision check: generate a random 6-7 char string,
 *      query the DB to see if it exists, retry if it does. Simple, but
 *      wastes a DB round trip on every collision, and collisions get more
 *      likely as the table grows (birthday paradox).
 *   2) Hashing (MD5/SHA-256 truncated): deterministic, but truncating a
 *      hash reintroduces collisions, and now you need collision-handling
 *      logic anyway - so you get hashing's complexity without avoiding
 *      the problem it was meant to solve.
 *   3) Base62 encode the auto-increment id (what's implemented here):
 *      the id is already guaranteed unique by the database, so encoding
 *      it can NEVER collide. No retries, no extra DB lookups. The only
 *      downside: short codes are sequentially guessable (id 100 encodes
 *      to a similar-looking code to id 101), which is a real drawback if
 *      you need protection against enumeration - a valid follow-up point
 *      to raise if an interviewer pushes on trade-offs.
 *
 * WHY BASE62 SPECIFICALLY:
 *   URL-safe characters only: 0-9, a-z, A-Z = 62 characters, no '+' or '/'
 *   like Base64 uses (those need URL-encoding to be safe in a link).
 *   Base62 also produces shorter strings than plain decimal for the same
 *   number, which is the whole point of a "short" URL:
 *     id 1,000,000 in decimal = 7 characters ("1000000")
 *     id 1,000,000 in Base62  = 4 characters ("4c92")
 */
@Service
public class UrlShortenerService {

    private static final String ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length(); // 62

    private final UrlMappingRepository repository;

    public UrlShortenerService(UrlMappingRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates a short code for the given long URL.
     *
     * Flow:
     *  1. Validate and normalize the destination URL.
     *  2. Save a new row with a placeholder, so the DB assigns an
     *     auto-increment id.
     *  3. Encode that id to Base62 -> this becomes the shortCode.
     *  4. Update the row with the real shortCode and save again.
     *
     * Step 2+4 (save twice) is necessary because we don't know the id until
     * AFTER the first insert - the encoding depends on a value the DB itself
     * generates. Each request creates its own link so expiry settings are
     * never silently changed by reusing another link for the same URL.
     */
    @Transactional
    public UrlMapping createShortUrl(String originalUrl, LocalDateTime expiresAt) {
        String validatedUrl = validateOriginalUrl(originalUrl);
        if (expiresAt != null && !expiresAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Expiry must be in the future");
        }

        UrlMapping mapping = new UrlMapping();
        mapping.setOriginalUrl(validatedUrl);
        mapping.setExpiresAt(expiresAt);
        // A unique placeholder avoids collisions while concurrent inserts wait
        // for the database-generated ID used by the final Base62 code.
        mapping.setShortCode(UUID.randomUUID().toString().replace("-", "").substring(0, 12));

        UrlMapping saved = repository.save(mapping); // id gets generated here

        saved.setShortCode(encode(saved.getId()));
        return repository.save(saved);
    }

    /**
     * Looks up the original URL for a short code and increments the click
     * counter. Throws if not found or expired - the controller translates
     * that into a 404.
     */
    @Transactional
    public UrlMapping resolveAndTrackClick(String shortCode) {
        if (!isValidShortCode(shortCode)) {
            throw new ShortCodeNotFoundException(shortCode);
        }

        int updated = repository.incrementClickCountIfActive(shortCode, LocalDateTime.now());
        if (updated == 0) {
            throw new ShortCodeNotFoundException(shortCode);
        }
        return repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
    }

    public UrlMapping getStats(String shortCode) {
        if (!isValidShortCode(shortCode)) {
            throw new ShortCodeNotFoundException(shortCode);
        }
        return repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
    }

    private boolean isValidShortCode(String shortCode) {
        return shortCode != null && !shortCode.isEmpty() && shortCode.length() <= 12
                && shortCode.matches("[0-9a-zA-Z]+");
    }

    private String validateOriginalUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank() || originalUrl.length() > 2048) {
            throw new IllegalArgumentException("Provide a URL between 1 and 2048 characters");
        }

        String value = originalUrl.trim();
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (!uri.isAbsolute() || uri.getHost() == null || uri.getHost().isBlank()
                    || uri.getUserInfo() != null || scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("Please provide a valid http(s) URL");
            }
            return uri.toASCIIString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Please provide a valid http(s) URL");
        }
    }

    /**
     * Converts a positive long into a Base62 string.
     * Same idea as converting decimal -> binary, just with 62 digits
     * instead of 2. Repeatedly divide by 62, the remainder at each step
     * is the next digit (read in reverse at the end).
     *
     * Example: id = 125 -> 125 / 62 = 2 remainder 1, then 2 / 62 = 0
     * remainder 2. Reading the remainders in reverse gives "21".
     */
    String encode(long id) {
        if (id < 0) {
            throw new IllegalArgumentException("ID must not be negative");
        }
        StringBuilder sb = new StringBuilder();
        if (id == 0) return String.valueOf(ALPHABET.charAt(0));

        while (id > 0) {
            int remainder = (int) (id % BASE);
            sb.append(ALPHABET.charAt(remainder));
            id /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * Reverse of encode() - not currently called anywhere in the app, but
     * worth having (and understanding) because interviewers sometimes ask
     * "how would you decode a short code back to the id?"
     */
    long decode(String shortCode) {
        if (shortCode == null || shortCode.isEmpty()) {
            throw new IllegalArgumentException("Short code must not be empty");
        }
        long id = 0;
        for (char c : shortCode.toCharArray()) {
            int digit = ALPHABET.indexOf(c);
            if (digit < 0) {
                throw new IllegalArgumentException("Short code contains an invalid character");
            }
            id = Math.addExact(Math.multiplyExact(id, BASE), digit);
        }
        return id;
    }
}
