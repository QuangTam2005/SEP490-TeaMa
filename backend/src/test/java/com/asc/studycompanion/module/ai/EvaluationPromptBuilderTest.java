package com.asc.studycompanion.module.ai;

import com.asc.studycompanion.module.ai.config.AiProperties;
import com.asc.studycompanion.module.ai.provider.AiEvaluationInput;
import com.asc.studycompanion.module.ai.provider.EvaluationPromptBuilder;
import com.asc.studycompanion.module.ai.provider.SearchSnippet;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluationPromptBuilderTest {

    @Test
    void buildsPiiFreePromptEnforcingKnttTextbookAndContextBudget() {
        AiProperties props = new AiProperties();
        props.setMaxContextTokens(2048);
        EvaluationPromptBuilder builder = new EvaluationPromptBuilder(props);

        String system = builder.buildSystemPrompt();
        assertTrue(system.contains("Kết nối tri thức với cuộc sống"));
        assertTrue(system.contains("Wikipedia"));
        assertTrue(system.contains("INSUFFICIENT_EVIDENCE"));
        assertTrue(system.contains("600"));

        AiEvaluationInput input = new AiEvaluationInput(
                8,
                SubjectCode.MATH,
                "Toán",
                StudyTaskType.REVIEW,
                "Hằng đẳng thức đáng nhớ",
                "a".repeat(900),
                List.of(new SearchSnippet(
                        "Toán 8 Kết nối tri thức - Bài 6",
                        "https://loigiaihay.com/ket-noi-tri-thuc",
                        "Hiệu hai bình phương: a^2 - b^2 = (a - b)(a + b).",
                        Instant.now()
                ))
        );

        String userPrompt = builder.buildUserPrompt(input);
        assertTrue(userPrompt.contains("Lớp 8"));
        assertTrue(userPrompt.contains("Toán"));
        assertTrue(userPrompt.contains("Kết nối tri thức"));
        assertFalse(userPrompt.contains("@gmail.com"));
        assertTrue(userPrompt.length() <= 2048 * 3 + 50);
    }
}
