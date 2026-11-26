package com.asc.studycompanion.module.guardian.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload when a Guardian confirms completion or requests revision on a Drive-submitted custom task.
 */
public record GuardianVerifyCustomTaskRequest(
        @NotNull(message = "Vui lòng chọn xác nhận đạt hoặc nhắc con bổ sung")
        Boolean approved,
        @Size(max = 300, message = "Lời nhắn phụ huynh tối đa 300 ký tự")
        String note
) {
}
