package com.asc.studycompanion.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed JWT properties bound from app.jwt.* in application.yml.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret;
    private long expirationMs = 86_400_000L;
}
