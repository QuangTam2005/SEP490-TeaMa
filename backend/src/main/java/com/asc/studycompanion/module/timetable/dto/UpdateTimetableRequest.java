package com.asc.studycompanion.module.timetable.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request body for PUT /api/student/timetable.
 */
public record UpdateTimetableRequest(
        @NotNull(message = "Danh sách tiết học không được để trống")
        List<@NotNull(message = "Phần tử tiết học không được để trống") @Valid TimetableEntryDto> entries
) {
}
