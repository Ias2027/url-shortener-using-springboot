package com.placement.urlshortener.repository;

import com.placement.urlshortener.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA generates the implementation of this interface at runtime.
 * Method names are parsed into SQL automatically:
 *   findByShortCode(x)   -> SELECT * FROM url_mapping WHERE short_code = x
 *   findByOriginalUrl(x) -> SELECT * FROM url_mapping WHERE original_url = x
 *
 * If asked in an interview: "How does Spring know what query to run?" ->
 * it parses the method name against the entity's field names (query derivation).
 * For anything more complex you'd write a @Query with JPQL/native SQL.
 */
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortCode(String shortCode);

    // Used to avoid creating duplicate short codes for the same long URL.
    Optional<UrlMapping> findByOriginalUrl(String originalUrl);
}
