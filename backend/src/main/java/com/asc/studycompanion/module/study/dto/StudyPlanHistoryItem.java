package com.asc.studycompanion.module.study.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Historical daily study plan snapshot returned by GET /api/student/plans/history.
 */
public record StudyPlanHistoryItem(
        Long planId,
        LocalDate targetDate,
        String weekdayLabel,
        int totalTasks,
        int completedTasks,
        List<NextDaySubjectPair> subjects
) {
}
