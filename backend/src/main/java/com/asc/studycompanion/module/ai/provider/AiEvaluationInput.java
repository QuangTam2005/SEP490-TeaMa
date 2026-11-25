package com.asc.studycompanion.module.ai.provider;

import com.asc.studycompanion.module.ai.config.AiProperties;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;

import java.util.List;

/**
 * Strictly PII-free evaluation input passed to AI providers (never includes name, email, or userId).
 */
public record AiEvaluationInput(
        int grade,
        SubjectCode subjectCode,
        String subjectName,
        StudyTaskType taskType,
        String lessonTitle,
        String studentText,
        List<SearchSnippet> sources,
        String textbookSeries,
        String textbookEdition
) {
    public AiEvaluationInput(
            int grade,
            SubjectCode subjectCode,
            String subjectName,
            StudyTaskType taskType,
            String lessonTitle,
            String studentText,
            List<SearchSnippet> sources
    ) {
        this(
                grade,
                subjectCode,
                subjectName,
                taskType,
                lessonTitle,
                studentText,
                sources,
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
