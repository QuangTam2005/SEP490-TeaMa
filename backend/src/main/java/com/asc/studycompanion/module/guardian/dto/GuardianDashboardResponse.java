package com.asc.studycompanion.module.guardian.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Guardian Dashboard DTO matching Frontend GuardianDashboardResponse contract.
 */
public record GuardianDashboardResponse(
        String linkedStudentName,
        Integer grade,
        LocalDate selectedDate,
        String selectedSubject,
        int totalTasks,
        int completedTasks,
        int evaluatingTasks,
        List<GuardianTaskItem> items,
        String defaultDriveFolderUrl
) {
    public GuardianDashboardResponse(
            String linkedStudentName,
            Integer grade,
            LocalDate selectedDate,
            String selectedSubject,
            int totalTasks,
            int completedTasks,
            int evaluatingTasks,
            List<GuardianTaskItem> items
    ) {
        this(
                linkedStudentName,
                grade,
                selectedDate,
                selectedSubject,
                totalTasks,
                completedTasks,
                evaluatingTasks,
                items,
                null
        );
    }
}
