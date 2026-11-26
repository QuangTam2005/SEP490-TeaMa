package com.asc.studycompanion.module.study.dto;

import jakarta.validation.constraints.Size;

/**
 * Student confirmation payload for completing a Guardian-assigned custom task (with optional note or Drive file URL).
 */
public record StudentCustomSubmitRequest(
        @Size(max = 300, message = "Ghi chú nộp bài tối đa 300 ký tự")
        String submissionNote,
        @Size(max = 500, message = "Link file Google Drive tối đa 500 ký tự")
        String driveFileUrl
) {
}
