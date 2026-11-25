package com.asc.studycompanion.module.auth.dto;

import com.asc.studycompanion.module.auth.entity.UserRole;

import java.time.Instant;

/**
 * Public user profile DTO matching Frontend UserResponse contract (excludes passwordHash).
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        UserRole role,
        Integer grade,
        String linkedStudentName,
        Instant createdAt
) {
}
