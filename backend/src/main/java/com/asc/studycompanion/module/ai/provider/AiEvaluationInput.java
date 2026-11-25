package com.asc.studycompanion.module.ai.provider;

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
        List<SearchSnippet> sources
) {
}
