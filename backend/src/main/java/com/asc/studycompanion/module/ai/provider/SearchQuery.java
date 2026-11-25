package com.asc.studycompanion.module.ai.provider;

import com.asc.studycompanion.module.timetable.entity.SubjectCode;

/**
 * PII-free search query payload (grade, subject, lessonTitle, and optional excerpt of lesson content).
 */
public record SearchQuery(
        int grade,
        SubjectCode subjectCode,
        String subjectName,
        String lessonTitle,
        String submissionContent
) {
    public SearchQuery(int grade, SubjectCode subjectCode, String subjectName, String lessonTitle) {
        this(grade, subjectCode, subjectName, lessonTitle, "");
    }
}
