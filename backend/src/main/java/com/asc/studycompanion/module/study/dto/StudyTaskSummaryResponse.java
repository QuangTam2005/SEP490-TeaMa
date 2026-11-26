package com.asc.studycompanion.module.study.dto;

import com.asc.studycompanion.module.ai.entity.OverallEvaluationStatus;
import com.asc.studycompanion.module.study.entity.SkipRequestStatus;
import com.asc.studycompanion.module.study.entity.StudyTask;
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
        String guardianNote,
        SkipRequestStatus skipRequestStatus,
        String skipRequestReason,
        Instant skipRequestedAt,
        Instant skipReviewedAt
) {
    public static StudyTaskSummaryResponse from(
            StudyTask task,
            String currentLessonTitle,
            OverallEvaluationStatus latestEvaluationStatus
    ) {
        return new StudyTaskSummaryResponse(
                task.getId(),
                task.getSubject().getCode(),
                task.getSubject().getName(),
                task.getTaskType(),
                task.getStatus(),
                task.getRevisionCount(),
                StudyTask.MAX_REVISIONS,
                currentLessonTitle,
                latestEvaluationStatus,
                task.getGuardianAcknowledgedAt(),
                task.getGuardianNote(),
                task.getSkipRequestStatus(),
                task.getSkipRequestReason(),
                task.getSkipRequestedAt(),
                task.getSkipReviewedAt()
        );
    }
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
                null,
                null,
                null,
                null,
                null
        );
    }

    public StudyTaskSummaryResponse(
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
                guardianAcknowledgedAt,
                guardianNote,
                null,
                null,
                null,
                null
        );
    }
}
