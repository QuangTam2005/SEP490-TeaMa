package com.asc.studycompanion.module.guardian.dto;

import com.asc.studycompanion.module.ai.entity.AccuracyLevel;
import com.asc.studycompanion.module.ai.entity.CompletenessLevel;
import com.asc.studycompanion.module.ai.entity.OverallEvaluationStatus;

import java.time.Instant;

/**
 * Compact AI evaluation summary for GuardianTaskItem (never exposes AIUsageLog cost/tokens).
 */
public record GuardianEvaluationSummary(
        AccuracyLevel accuracy,
        CompletenessLevel completeness,
        OverallEvaluationStatus overallStatus,
        String feedback,
        Instant evaluatedAt
) {
}
