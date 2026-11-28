package com.asc.studycompanion.module.quiz.dto;

import com.asc.studycompanion.module.quiz.entity.DailyQuizSessionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record StudentDailyQuizSessionResponse(
        Long sessionId,
        LocalDate sessionDate,
        DailyQuizSessionStatus status,
        int totalQuestions,
        int answeredCount,
        int correctCount,
        int accuracyPercent,
        int cycleStartIndex,
        int cycleEndIndex,
        Instant completedAt,
        List<StudentDailyQuizItemResponse> items
) {
}
