package com.asc.studycompanion.module.study.dto;

import java.time.LocalDate;

/**
 * Consecutive study streak summary returned by GET /api/student/streak.
 */
public record StudentStreakResponse(
        int currentStreak,
        int longestStreak,
        int totalCompletedDays,
        LocalDate lastActiveDate
) {
}
