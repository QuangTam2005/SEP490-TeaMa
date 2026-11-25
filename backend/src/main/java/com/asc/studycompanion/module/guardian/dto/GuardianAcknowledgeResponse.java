package com.asc.studycompanion.module.guardian.dto;

import java.time.Instant;

/**
 * Confirmation payload returned by POST /api/guardian/tasks/{taskId}/acknowledge.
 */
public record GuardianAcknowledgeResponse(
        Long taskId,
        Instant acknowledgedAt,
        String note
) {
}
