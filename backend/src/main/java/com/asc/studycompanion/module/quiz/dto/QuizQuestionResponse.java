package com.asc.studycompanion.module.quiz.dto;

import com.asc.studycompanion.module.quiz.entity.QuizOption;

import java.time.Instant;

public record QuizQuestionResponse(
        Long id,
        int sortOrder,
        String questionText,
        String optionA,
        String optionB,
        QuizOption correctOption,
        String explanation,
        String topicLabel,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
