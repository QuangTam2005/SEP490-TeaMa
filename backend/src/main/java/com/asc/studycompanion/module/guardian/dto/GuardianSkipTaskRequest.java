package com.asc.studycompanion.module.guardian.dto;

import jakarta.validation.constraints.Size;

public record GuardianSkipTaskRequest(
        boolean skipped,
        @Size(max = 300, message = "Ghi chú tối đa 300 ký tự")
        String note
) {
}
