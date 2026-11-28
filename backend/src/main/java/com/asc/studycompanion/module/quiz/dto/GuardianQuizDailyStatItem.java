package com.asc.studycompanion.module.quiz.dto;

import com.asc.studycompanion.module.quiz.entity.DailyQuizSessionStatus;

import java.time.Instant;
import java.time.LocalDate;

public record GuardianQuizDailyStatItem(
        Long sessionId,
        LocalDate date,
        DailyQuizSessionStatus status,
        int totalQuestions,
        int answeredCount,
        int correctCount,
        int accuracyPercent,
        String cycleLabel,
        Instant completedAt
) {
}
