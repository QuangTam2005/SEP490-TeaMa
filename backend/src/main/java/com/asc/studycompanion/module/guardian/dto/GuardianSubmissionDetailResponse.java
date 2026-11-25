package com.asc.studycompanion.module.guardian.dto;

import com.asc.studycompanion.module.study.dto.StudySubmissionResponse;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Immutable submission history DTO matching Frontend GuardianSubmissionDetailResponse contract.
 */
public record GuardianSubmissionDetailResponse(
        Long submissionId,
        Long taskId,
        String studentName,
        LocalDate targetDate,
        SubjectCode subjectCode,
        String subjectName,
        StudyTaskType taskType,
        StudyTaskStatus status,
        Integer revisionCount,
        Instant guardianAcknowledgedAt,
        String guardianNote,
        List<StudySubmissionResponse> versions
) {
    public GuardianSubmissionDetailResponse(
            Long submissionId,
            String studentName,
            LocalDate targetDate,
            SubjectCode subjectCode,
            String subjectName,
            StudyTaskType taskType,
            StudyTaskStatus status,
            Integer revisionCount,
            List<StudySubmissionResponse> versions
    ) {
        this(
                submissionId,
                null,
                studentName,
                targetDate,
                subjectCode,
                subjectName,
                taskType,
                status,
                revisionCount,
                null,
                null,
                versions
        );
    }
}
