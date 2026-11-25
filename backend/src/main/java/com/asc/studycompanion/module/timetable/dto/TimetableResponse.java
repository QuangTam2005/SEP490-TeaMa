package com.asc.studycompanion.module.timetable.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO matching Frontend TimetableResponse contract.
 */
public record TimetableResponse(
        Long id,
        Integer version,
        LocalDate effectiveDate,
        Instant updatedAt,
        String updatedByName,
        List<TimetableEntryDto> entries
) {
}
