package com.asc.studycompanion.module.ai.provider;

import java.time.Instant;

/**
 * Compact grounded web source snippet retrieved for AI verification.
 */
public record SearchSnippet(
        String title,
        String url,
        String snippet,
        Instant retrievedAt
) {
}
