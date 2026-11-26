package com.asc.studycompanion.module.guardian.dto;

import com.asc.studycompanion.module.study.entity.SkipRequestStatus;
import com.asc.studycompanion.module.study.entity.StudyTask;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.study.entity.TaskAssignmentMode;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Dashboard row DTO matching Frontend GuardianTaskItem contract.
 */
public record GuardianTaskItem(
        Long taskId,
        Long currentSubmissionId,
        LocalDate targetDate,
        SubjectCode subjectCode,
        String subjectName,
        StudyTaskType taskType,
        StudyTaskStatus status,
        String lessonTitle,
        Integer revisionCount,
        GuardianEvaluationSummary evaluation,
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
    public static GuardianTaskItem from(
            StudyTask task,
            Long currentSubmissionId,
            String lessonTitle,
            GuardianEvaluationSummary evaluation
    ) {
        return new GuardianTaskItem(
                task.getId(),
                currentSubmissionId,
                task.getPlan() != null ? task.getPlan().getTargetDate() : null,
                task.getSubject().getCode(),
                task.getSubject().getName(),
                task.getTaskType(),
                task.getStatus(),
                lessonTitle,
                task.getRevisionCount(),
                evaluation,
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

    public GuardianTaskItem(
            Long taskId,
            Long currentSubmissionId,
            LocalDate targetDate,
            SubjectCode subjectCode,
            String subjectName,
            StudyTaskType taskType,
            StudyTaskStatus status,
            String lessonTitle,
            Integer revisionCount,
            GuardianEvaluationSummary evaluation
    ) {
        this(
                taskId,
                currentSubmissionId,
                targetDate,
                subjectCode,
                subjectName,
                taskType,
                status,
                lessonTitle,
                revisionCount,
                evaluation,
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
}
