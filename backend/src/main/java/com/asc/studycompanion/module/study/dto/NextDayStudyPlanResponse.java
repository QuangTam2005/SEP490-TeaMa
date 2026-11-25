package com.asc.studycompanion.module.study.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Next-day study plan DTO matching Frontend NextDayStudyPlanResponse contract.
 */
public record NextDayStudyPlanResponse(
        Long planId,
        LocalDate targetDate,
        String weekdayLabel,
        boolean hasSchedule,
        List<NextDaySubjectPair> subjects
) {
}
