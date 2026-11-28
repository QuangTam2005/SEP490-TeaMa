package com.asc.studycompanion.module.quiz.repository;

import com.asc.studycompanion.module.quiz.entity.DailyQuizSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyQuizSessionRepository extends JpaRepository<DailyQuizSession, Long> {

    Optional<DailyQuizSession> findByStudentIdAndSessionDate(Long studentId, LocalDate sessionDate);

    Optional<DailyQuizSession> findByIdAndStudentId(Long id, Long studentId);

    List<DailyQuizSession> findByStudentIdAndSessionDateBetweenOrderBySessionDateDesc(
            Long studentId,
            LocalDate fromDate,
            LocalDate toDate
    );

    List<DailyQuizSession> findByStudentIdOrderBySessionDateDesc(Long studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DailyQuizSession s where s.id = :id")
    Optional<DailyQuizSession> findByIdForUpdate(@Param("id") Long id);
}
