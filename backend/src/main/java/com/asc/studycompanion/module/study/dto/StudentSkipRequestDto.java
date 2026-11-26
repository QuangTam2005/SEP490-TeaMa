package com.asc.studycompanion.module.study.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload when a Student requests Guardian permission to skip a REVIEW or PREVIEW task.
 */
public record StudentSkipRequestDto(
        @NotBlank(message = "Con hãy nhập lý do xin bỏ qua bài học nhé")
        @Size(min = 5, max = 300, message = "Lý do xin bỏ qua phải từ 5 đến 300 ký tự")
        String reason
) {
}
