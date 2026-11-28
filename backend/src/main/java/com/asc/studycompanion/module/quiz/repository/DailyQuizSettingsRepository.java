package com.asc.studycompanion.module.quiz.repository;

import com.asc.studycompanion.module.quiz.entity.DailyQuizSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DailyQuizSettingsRepository extends JpaRepository<DailyQuizSettings, Long> {

    Optional<DailyQuizSettings> findByStudentId(Long studentId);
}
