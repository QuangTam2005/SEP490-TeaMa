package com.asc.studycompanion.module.ai.dto;

import com.asc.studycompanion.module.ai.entity.AccuracyLevel;
import com.asc.studycompanion.module.ai.entity.CompletenessLevel;
import com.asc.studycompanion.module.ai.entity.ConfidenceLevel;
import com.asc.studycompanion.module.ai.entity.OverallEvaluationStatus;

import java.time.Instant;
import java.util.List;

/**
 * AI evaluation DTO matching Frontend AIEvaluationResponse contract.
 */
public record AIEvaluationResponse(
        Long id,
        Long submissionId,
        AccuracyLevel accuracy,
        CompletenessLevel completeness,
        OverallEvaluationStatus overallStatus,
        String feedback,
        List<String> incorrectPoints,
        List<String> missingPoints,
        ConfidenceLevel confidence,
        List<SourceReferenceResponse> sources,
        Instant evaluatedAt
) {
}
