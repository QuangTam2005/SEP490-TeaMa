package com.asc.studycompanion.module.study.repository;

import com.asc.studycompanion.module.study.entity.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {

    Optional<StudyPlan> findByStudentIdAndTargetDate(Long studentId, LocalDate targetDate);
}
