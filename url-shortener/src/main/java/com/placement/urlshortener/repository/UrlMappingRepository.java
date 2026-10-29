package com.placement.urlshortener.repository;

import com.placement.urlshortener.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Spring Data JPA generates the implementation of this interface at runtime.
 * Method names are parsed into SQL automatically:
 *   findByShortCode(x)   -> SELECT * FROM url_mapping WHERE short_code = x
 *
 * If asked in an interview: "How does Spring know what query to run?" ->
 * it parses the method name against the entity's field names (query derivation).
 * For anything more complex you'd write a @Query with JPQL/native SQL.
 */
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortCode(String shortCode);

    @Modifying
    @Query("update UrlMapping u set u.clickCount = u.clickCount + 1 "
            + "where u.shortCode = :shortCode and (u.expiresAt is null or u.expiresAt > :now)")
    int incrementClickCountIfActive(@Param("shortCode") String shortCode,
                                    @Param("now") LocalDateTime now);
}
