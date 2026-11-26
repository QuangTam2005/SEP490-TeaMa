package com.asc.studycompanion.module.guardian.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload when a Guardian approves or rejects a Student's skip request.
 */
public record GuardianSkipReviewRequest(
        @NotNull(message = "Vui lòng chọn đồng ý hoặc từ chối yêu cầu bỏ qua")
        Boolean approved,
        @Size(max = 300, message = "Lời nhắn của phụ huynh tối đa 300 ký tự")
        String note
) {
}
