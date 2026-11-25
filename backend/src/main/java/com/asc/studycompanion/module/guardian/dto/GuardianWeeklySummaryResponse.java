package com.asc.studycompanion.module.guardian.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Weekly study progress aggregation returned by GET /api/guardian/weekly (excludes AI cost telemetry per BR-22).
 */
public record GuardianWeeklySummaryResponse(
        Long studentId,
        String studentName,
        Integer grade,
        LocalDate weekStart,
        LocalDate weekEnd,
        int totalTasks,
        int completedTasks,
        int acknowledgedTasks,
        List<GuardianWeeklyDaySummary> days,
        List<GuardianWeeklySubjectStat> subjects
) {
}
