package com.asc.studycompanion.module.study.dto;

import com.asc.studycompanion.module.ai.entity.OverallEvaluationStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;

import java.time.Instant;

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
        OverallEvaluationStatus latestEvaluationStatus,
        Instant guardianAcknowledgedAt,
        String guardianNote
) {
    public StudyTaskSummaryResponse(
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
        this(
                id,
                subjectCode,
                subjectName,
                taskType,
                status,
                revisionCount,
                maxRevisions,
                currentLessonTitle,
                latestEvaluationStatus,
                null,
                null
        );
    }
}
