package com.asc.studycompanion.module.guardian.dto;

import com.asc.studycompanion.module.study.entity.SkipRequestStatus;
import com.asc.studycompanion.module.study.entity.StudyTaskStatus;

import java.time.Instant;

public record GuardianSkipTaskResponse(
        Long taskId,
        StudyTaskStatus status,
        String guardianNote,
        SkipRequestStatus skipRequestStatus,
        String skipRequestReason,
        Instant skipRequestedAt,
        Instant skipReviewedAt
) {
    public GuardianSkipTaskResponse(Long taskId, StudyTaskStatus status, String guardianNote) {
        this(taskId, status, guardianNote, null, null, null, null);
    }
}
