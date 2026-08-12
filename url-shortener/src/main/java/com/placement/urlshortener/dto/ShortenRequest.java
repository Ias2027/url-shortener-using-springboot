package com.placement.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(
        regexp = "^(https?://)[\\w.-]+(\\.[a-zA-Z]{2,})+.*$",
        message = "Please provide a valid http(s) URL"
    )
    private String url;

    // Optional: minutes from now until this link expires. Null = never.
    private Long expiresInMinutes;
}
