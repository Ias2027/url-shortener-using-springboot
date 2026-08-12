package com.placement.urlshortener.service;

import com.placement.urlshortener.exception.ShortCodeNotFoundException;
import com.placement.urlshortener.model.UrlMapping;
import com.placement.urlshortener.repository.UrlMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

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
     * Creates (or reuses) a short code for the given long URL.
     *
     * Flow:
     *  1. If this exact URL was already shortened before, return the existing
     *     short code instead of creating a duplicate row (saves space, and
     *     means the same long URL always maps to the same short URL).
     *  2. Otherwise save a new row with a placeholder, so the DB assigns an
     *     auto-increment id.
     *  3. Encode that id to Base62 -> this becomes the shortCode.
     *  4. Update the row with the real shortCode and save again.
     *
     * Step 2+4 (save twice) is necessary because we don't know the id until
     * AFTER the first insert - the encoding depends on a value the DB itself
     * generates. This is a common, honest talking point in interviews.
     */
    @Transactional
    public UrlMapping createShortUrl(String originalUrl, LocalDateTime expiresAt) {
        Optional<UrlMapping> existing = repository.findByOriginalUrl(originalUrl);
        if (existing.isPresent()) {
            return existing.get();
        }

        UrlMapping mapping = new UrlMapping();
        mapping.setOriginalUrl(originalUrl);
        mapping.setExpiresAt(expiresAt);
        mapping.setShortCode(""); // temporary, filled in below once we have an id

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
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));

        if (mapping.getExpiresAt() != null && mapping.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ShortCodeNotFoundException(shortCode);
        }

        mapping.setClickCount(mapping.getClickCount() + 1);
        return repository.save(mapping);
    }

    public UrlMapping getStats(String shortCode) {
        return repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
    }

    /**
     * Converts a positive long into a Base62 string.
     * Same idea as converting decimal -> binary, just with 62 digits
     * instead of 2. Repeatedly divide by 62, the remainder at each step
     * is the next digit (read in reverse at the end).
     *
     * Example: id = 125
     *   125 / 62 = 2 remainder 1  -> digit 'b' (index 1... wait, index math below)
     *   2   / 62 = 0 remainder 2  -> digit 'c'  eventually built as "b" prefixed
     * (Walk through this by hand once with a small number - it's the
     * single most likely thing you'll be asked to explain on a whiteboard.)
     */
    String encode(long id) {
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
        long id = 0;
        for (char c : shortCode.toCharArray()) {
            id = id * BASE + ALPHABET.indexOf(c);
        }
        return id;
    }
}
