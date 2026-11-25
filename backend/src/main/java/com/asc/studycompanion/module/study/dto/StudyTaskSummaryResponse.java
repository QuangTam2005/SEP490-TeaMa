package com.asc.studycompanion.module.study.dto;

import com.asc.studycompanion.module.ai.entity.OverallEvaluationStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;

/**
 * Summary DTO matching Frontend StudyTaskSummaryResponse contract.
 */
public record StudyTaskSummaryResponse(
        Long id,
        SubjectCode subjectCode,
        String subjectName,
        StudyTaskType taskType,
        StudyTaskStatus status,
        Integer revisionCount,
        Integer maxRevisions,
        String currentLessonTitle,
        OverallEvaluationStatus latestEvaluationStatus
) {
}
