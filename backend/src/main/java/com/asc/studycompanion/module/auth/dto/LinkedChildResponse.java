package com.asc.studycompanion.module.auth.dto;

import java.time.Instant;

/**
 * Represents a Student actively linked to a Guardian account for multi-child selection.
 */
public record LinkedChildResponse(
        Long studentId,
        Long studentUserId,
        String displayName,
        Integer grade,
        Instant linkedAt
) {
}
