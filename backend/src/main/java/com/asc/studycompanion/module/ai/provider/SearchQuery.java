package com.asc.studycompanion.module.ai.provider;

import com.asc.studycompanion.module.timetable.entity.SubjectCode;

/**
 * PII-free search query payload (contains grade, subject, lessonTitle only).
 */
public record SearchQuery(
        int grade,
        SubjectCode subjectCode,
        String subjectName,
        String lessonTitle
) {
}
