package com.asc.studycompanion.module.study.dto;

import com.asc.studycompanion.module.timetable.entity.SubjectCode;

/**
 * Subject task pair DTO matching Frontend NextDaySubjectPair contract.
 */
public record NextDaySubjectPair(
        SubjectCode subjectCode,
        String subjectName,
        String periodNote,
        StudyTaskSummaryResponse reviewTask,
        StudyTaskSummaryResponse previewTask
) {
}
