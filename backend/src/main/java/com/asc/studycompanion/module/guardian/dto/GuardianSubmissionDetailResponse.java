package com.asc.studycompanion.module.guardian.dto;

import com.asc.studycompanion.module.study.dto.StudySubmissionResponse;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;

import java.time.LocalDate;
import java.util.List;

/**
 * Immutable submission history DTO matching Frontend GuardianSubmissionDetailResponse contract.
 */
public record GuardianSubmissionDetailResponse(
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
}
