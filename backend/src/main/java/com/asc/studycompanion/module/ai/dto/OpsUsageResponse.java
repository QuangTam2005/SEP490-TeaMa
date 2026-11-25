package com.asc.studycompanion.module.ai.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Operational AI cost and token telemetry summary gated strictly behind X-Ops-Secret (BR-22).
 */
public record OpsUsageResponse(
        LocalDate fromDate,
        LocalDate toDate,
        int totalCalls,
        int cacheHits,
        int failedCalls,
        long totalInputTokens,
        long totalOutputTokens,
        long totalSearchCount,
        BigDecimal totalEstimatedCostUsd,
        List<ModelUsageBreakdown> byModel
) {
    public record ModelUsageBreakdown(
            String provider,
            String model,
            int calls,
            int cacheHits,
            long inputTokens,
            long outputTokens,
            BigDecimal estimatedCostUsd
    ) {
    }
}
