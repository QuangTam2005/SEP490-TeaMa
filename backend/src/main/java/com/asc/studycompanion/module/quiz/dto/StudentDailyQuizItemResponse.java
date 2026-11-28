package com.asc.studycompanion.module.quiz.dto;

import com.asc.studycompanion.module.quiz.entity.QuizOption;

import java.time.Instant;

/**
 * Student-facing question item inside today's quiz session.
 * Unanswered items strictly hide correctOption, correct, and explanation to avoid leaking answers.
 */
public record StudentDailyQuizItemResponse(
        Long id,
        int itemOrder,
        String questionText,
        String optionA,
        String optionB,
        String topicLabel,
        QuizOption selectedOption,
        QuizOption correctOption,
        Boolean correct,
        String explanation,
        Instant answeredAt
) {
}
