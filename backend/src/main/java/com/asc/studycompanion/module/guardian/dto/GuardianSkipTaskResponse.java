package com.asc.studycompanion.module.guardian.dto;

import com.asc.studycompanion.module.study.entity.StudyTaskStatus;

public record GuardianSkipTaskResponse(
        Long taskId,
        StudyTaskStatus status,
        String guardianNote
) {
}
