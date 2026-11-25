package com.asc.studycompanion.module.auth.dto;

/**
 * JWT authentication payload returned on register and login.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        UserResponse user
) {
}
