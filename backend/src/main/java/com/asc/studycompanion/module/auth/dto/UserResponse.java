package com.asc.studycompanion.module.auth.dto;

import java.time.Instant;

/**
 * Public user profile DTO (excludes passwordHash).
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        String studentCode,
        String phone,
        String avatarUrl,
        Instant createdAt
) {
}
