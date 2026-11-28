package com.asc.studycompanion.module.quiz.dto;

import com.asc.studycompanion.module.quiz.entity.QuizOption;

import java.time.Instant;

public record GuardianQuizSessionDetailItem(
        Long itemId,
        int itemOrder,
        Long questionId,
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
