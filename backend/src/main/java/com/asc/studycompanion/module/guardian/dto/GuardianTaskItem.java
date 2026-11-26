package com.asc.studycompanion.module.guardian.dto;

import com.asc.studycompanion.module.study.entity.SkipRequestStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
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
        Instant skipReviewedAt
) {
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
                null
        );
    }
}
