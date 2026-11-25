package com.asc.studycompanion.module.study.dto;

import com.asc.studycompanion.module.ai.dto.AIEvaluationResponse;

import java.time.Instant;

/**
 * Immutable submission DTO matching Frontend StudySubmissionResponse contract.
 */
public record StudySubmissionResponse(
        Long id,
        Long taskId,
        Integer version,
        boolean isCurrent,
        String lessonTitle,
        String content,
        Integer charCount,
        Instant submittedAt,
        AIEvaluationResponse evaluation,
        String lastError
) {
}
