package com.asc.studycompanion.module.study.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Task detail DTO matching Frontend StudyTaskDetailResponse contract.
 */
public record StudyTaskDetailResponse(
        StudyTaskSummaryResponse task,
        LocalDate targetDate,
        boolean reviewCompleted,
        List<StudySubmissionResponse> submissions
) {
}
