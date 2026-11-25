package com.asc.studycompanion.module.study.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for submitting a REVIEW or PREVIEW version matching Frontend SubmitStudyRequest.
 */
public record SubmitStudyRequest(
        @NotBlank(message = "Tên bài học không được để trống")
        @Size(max = 255, message = "Tên bài học tối đa 255 ký tự")
        String lessonTitle,

        @NotBlank(message = "Nội dung bài viết không được để trống")
        String content,

        @Size(max = 64, message = "Idempotency-Key tối đa 64 ký tự")
        String idempotencyKey
) {
    public SubmitStudyRequest {
        lessonTitle = lessonTitle != null ? lessonTitle.trim() : null;
        idempotencyKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey.trim()
                : null;
    }
}
