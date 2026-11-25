package com.asc.studycompanion.module.ai.dto;

import java.time.Instant;

/**
 * Source citation DTO matching Frontend SourceReferenceResponse contract.
 */
public record SourceReferenceResponse(
        Long id,
        String title,
        String url,
        String snippet,
        Instant retrievedAt
) {
}
