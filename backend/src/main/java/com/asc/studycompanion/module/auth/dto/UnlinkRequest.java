package com.asc.studycompanion.module.auth.dto;

/**
 * Optional request payload when unlinking a specific linked student (for multi-child Guardians).
 */
public record UnlinkRequest(
        Long studentId
) {
}
