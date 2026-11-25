package com.asc.studycompanion.module.study.repository;

import com.asc.studycompanion.module.study.entity.StudyTask;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudyTaskRepository extends JpaRepository<StudyTask, Long> {

    Optional<StudyTask> findByPlanIdAndSubjectIdAndTaskType(
            Long planId,
            Long subjectId,
            StudyTaskType taskType
    );
}
