package com.asc.studycompanion.module.ai.provider;

import com.asc.studycompanion.module.ai.config.AiProperties;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;

/**
 * PII-free search query payload anchored to grade, subject, textbook series/edition, and lesson title.
 */
public record SearchQuery(
        int grade,
        SubjectCode subjectCode,
        String subjectName,
        String lessonTitle,
        String submissionContent,
        String textbookSeries,
        String textbookEdition
) {
    public SearchQuery(int grade, SubjectCode subjectCode, String subjectName, String lessonTitle) {
        this(
                grade,
                subjectCode,
                subjectName,
                lessonTitle,
                "",
                defaultSeries(subjectCode),
                AiProperties.DEFAULT_TEXTBOOK_EDITION
        );
    }

    public SearchQuery(
            int grade,
            SubjectCode subjectCode,
            String subjectName,
            String lessonTitle,
            String submissionContent
    ) {
        this(
                grade,
                subjectCode,
                subjectName,
                lessonTitle,
                submissionContent,
                defaultSeries(subjectCode),
                AiProperties.DEFAULT_TEXTBOOK_EDITION
        );
    }

    public String resolvedTextbookSeries() {
        return textbookSeries != null && !textbookSeries.isBlank()
                ? textbookSeries.trim()
                : defaultSeries(subjectCode);
    }

    public String resolvedTextbookEdition() {
        return textbookEdition != null && !textbookEdition.isBlank()
                ? textbookEdition.trim()
                : AiProperties.DEFAULT_TEXTBOOK_EDITION;
    }

    private static String defaultSeries(SubjectCode code) {
        return code == SubjectCode.ENG
                ? AiProperties.ENGLISH_TEXTBOOK_SERIES
                : AiProperties.DEFAULT_TEXTBOOK_SERIES;
    }
}
