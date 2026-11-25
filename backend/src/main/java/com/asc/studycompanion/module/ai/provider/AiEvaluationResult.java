package com.asc.studycompanion.module.ai.provider;

import com.asc.studycompanion.module.ai.entity.AccuracyLevel;
import com.asc.studycompanion.module.ai.entity.CompletenessLevel;
import com.asc.studycompanion.module.ai.entity.ConfidenceLevel;
import com.asc.studycompanion.module.ai.entity.OverallEvaluationStatus;

import java.util.List;

/**
 * Parsed and validated structured result returned by AiEvaluationProvider.
 */
public record AiEvaluationResult(
        AccuracyLevel accuracy,
        CompletenessLevel completeness,
        OverallEvaluationStatus overallStatus,
        String feedback,
        List<String> incorrectPoints,
        List<String> missingPoints,
        ConfidenceLevel confidence,
        int inputTokens,
        int outputTokens,
        String modelUsed,
        String rawSanitizedJson
) {
}
