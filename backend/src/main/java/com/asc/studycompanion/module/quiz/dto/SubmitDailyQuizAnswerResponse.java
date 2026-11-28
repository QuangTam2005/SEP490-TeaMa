package com.asc.studycompanion.module.quiz.dto;

import com.asc.studycompanion.module.quiz.entity.DailyQuizSessionStatus;
import com.asc.studycompanion.module.quiz.entity.QuizOption;

import java.time.Instant;

public record SubmitDailyQuizAnswerResponse(
        Long sessionId,
        Long itemId,
        QuizOption selectedOption,
        QuizOption correctOption,
        boolean correct,
        String explanation,
        int answeredCount,
        int correctCount,
        int totalQuestions,
        int accuracyPercent,
        DailyQuizSessionStatus sessionStatus,
        boolean gateLocked,
        Instant completedAt
) {
}
