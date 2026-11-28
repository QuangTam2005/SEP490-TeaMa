package com.asc.studycompanion.module.quiz.dto;

import com.asc.studycompanion.module.quiz.entity.DailyQuizSessionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record GuardianQuizSessionDetailResponse(
        Long sessionId,
        Long studentId,
        String studentName,
        LocalDate sessionDate,
        DailyQuizSessionStatus status,
        int totalQuestions,
        int answeredCount,
        int correctCount,
        int accuracyPercent,
        String cycleLabel,
        Instant startedAt,
        Instant completedAt,
        List<GuardianQuizSessionDetailItem> items
) {
}
