package com.asc.studycompanion.module.guardian.dto;

import com.asc.studycompanion.module.timetable.entity.SubjectCode;

/**
 * Subject completion breakdown inside GuardianWeeklySummaryResponse.
 */
public record GuardianWeeklySubjectStat(
        SubjectCode subjectCode,
        String subjectName,
        int totalTasks,
        int completedTasks
) {
}
