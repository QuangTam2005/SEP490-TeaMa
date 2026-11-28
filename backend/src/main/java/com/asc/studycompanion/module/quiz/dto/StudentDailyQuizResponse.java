package com.asc.studycompanion.module.quiz.dto;

import java.time.LocalDate;

public record StudentDailyQuizResponse(
        Long studentId,
        LocalDate sessionDate,
        boolean enabled,
        boolean activeToday,
        boolean requiredToday,
        boolean gateLocked,
        String nextBatchLabel,
        StudentDailyQuizSessionResponse session
) {
}
