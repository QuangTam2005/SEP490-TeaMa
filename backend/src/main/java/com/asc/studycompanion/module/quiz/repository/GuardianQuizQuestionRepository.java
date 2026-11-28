package com.asc.studycompanion.module.quiz.repository;

import com.asc.studycompanion.module.quiz.entity.GuardianQuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuardianQuizQuestionRepository extends JpaRepository<GuardianQuizQuestion, Long> {

    List<GuardianQuizQuestion> findByStudentIdOrderBySortOrderAscIdAsc(Long studentId);

    List<GuardianQuizQuestion> findByStudentIdAndActiveTrueOrderBySortOrderAscIdAsc(Long studentId);

    Optional<GuardianQuizQuestion> findByIdAndStudentId(Long id, Long studentId);

    long countByStudentId(Long studentId);

    long countByStudentIdAndActiveTrue(Long studentId);
}
