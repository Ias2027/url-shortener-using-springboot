package com.placement.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * What the client sends us. Never expose the JPA entity directly in a
 * controller - DTOs decouple your API contract from your DB schema, so you
 * can change one without breaking the other. Good thing to say if asked
 * "why not just use the entity here?"
 */
@Getter
@Setter
public class ShortenRequest {

    @NotBlank(message = "URL must not be empty")
    @Size(max = 2048, message = "URL must be 2048 characters or fewer")
    private String url;

    // Optional: minutes from now until this link expires. Null = never.
    @Positive(message = "Expiry must be at least 1 minute")
    @Max(value = 525600, message = "Expiry cannot be more than 1 year")
    private Long expiresInMinutes;
}
