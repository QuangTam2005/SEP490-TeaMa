package com.asc.studycompanion.module.study.dto;

import com.asc.studycompanion.module.ai.entity.OverallEvaluationStatus;
import com.asc.studycompanion.module.study.entity.SkipRequestStatus;
import com.asc.studycompanion.module.study.entity.StudyTask;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.study.entity.TaskAssignmentMode;
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
        Instant skipReviewedAt,
        TaskAssignmentMode assignmentMode,
        String customTaskTitle,
        String customTaskDescription,
        String customDriveFolderUrl,
        Instant customAssignedAt,
        String customSubmissionNote,
        String customSubmissionUrl,
        Instant customSubmittedAt
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
                task.getSkipReviewedAt(),
                task.getAssignmentMode() != null ? task.getAssignmentMode() : TaskAssignmentMode.STANDARD_AI,
                task.getCustomTaskTitle(),
                task.getCustomTaskDescription(),
                task.getCustomDriveFolderUrl(),
                task.getCustomAssignedAt(),
                task.getCustomSubmissionNote(),
                task.getCustomSubmissionUrl(),
                task.getCustomSubmittedAt()
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
                null,
                TaskAssignmentMode.STANDARD_AI,
                null,
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
                null,
                TaskAssignmentMode.STANDARD_AI,
                null,
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
            String guardianNote,
            SkipRequestStatus skipRequestStatus,
            String skipRequestReason,
            Instant skipRequestedAt,
            Instant skipReviewedAt
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
                skipRequestStatus,
                skipRequestReason,
                skipRequestedAt,
                skipReviewedAt,
                TaskAssignmentMode.STANDARD_AI,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
