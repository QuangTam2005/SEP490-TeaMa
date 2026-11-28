package com.asc.studycompanion.module.quiz.repository;

import com.asc.studycompanion.module.quiz.entity.DailyQuizSessionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DailyQuizSessionItemRepository extends JpaRepository<DailyQuizSessionItem, Long> {

    List<DailyQuizSessionItem> findBySessionIdOrderByItemOrderAsc(Long sessionId);

    List<DailyQuizSessionItem> findBySessionIdInOrderBySessionIdAscItemOrderAsc(List<Long> sessionIds);
}
