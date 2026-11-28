package com.asc.studycompanion.module.quiz.dto;

import com.asc.studycompanion.module.quiz.entity.DailyQuizSessionStatus;

import java.util.List;

public record GuardianQuizStatsResponse(
        Long studentId,
        String studentName,
        long totalQuestionCount,
        long activeQuestionCount,
        int totalCompletedSessions,
        int totalQuestionsAnswered,
        int totalCorrectAnswers,
        int averageAccuracyPercent,
        int quizStreakDays,
        DailyQuizSessionStatus todayStatus,
        List<GuardianQuizDailyStatItem> dailyStats,
        List<GuardianHardestQuestionItem> hardestQuestions
) {
}
