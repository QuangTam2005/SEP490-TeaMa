package com.asc.studycompanion.module.quiz.dto;

import com.asc.studycompanion.module.quiz.entity.QuizSelectionMode;

import java.util.List;

public record GuardianQuizSettingsResponse(
        Long studentId,
        boolean enabled,
        int questionsPerDay,
        List<Integer> activeWeekdays,
        QuizSelectionMode selectionMode,
        boolean shuffleOptions,
        int rotationCursor,
        long totalQuestionCount,
        long activeQuestionCount,
        String nextBatchLabel
) {
}
