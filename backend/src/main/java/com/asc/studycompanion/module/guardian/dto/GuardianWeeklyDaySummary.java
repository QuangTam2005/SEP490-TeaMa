package com.asc.studycompanion.module.guardian.dto;

import java.time.LocalDate;

/**
 * Single day bucket inside GuardianWeeklySummaryResponse.
 */
public record GuardianWeeklyDaySummary(
        LocalDate date,
        String weekdayLabel,
        int totalTasks,
        int completedTasks,
        int acknowledgedTasks
) {
}
