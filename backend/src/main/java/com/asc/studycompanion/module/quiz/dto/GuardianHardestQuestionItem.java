package com.asc.studycompanion.module.quiz.dto;

public record GuardianHardestQuestionItem(
        Long questionId,
        String questionText,
        String topicLabel,
        int attemptCount,
        int wrongCount,
        int accuracyPercent
) {
}
