package com.asc.studycompanion.module.timetable.dto;

import com.asc.studycompanion.module.timetable.entity.SubjectCode;

/**
 * DTO matching Frontend SubjectResponse contract.
 */
public record SubjectResponse(
        Long id,
        SubjectCode code,
        String name
) {
}
