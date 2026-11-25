package com.asc.studycompanion.module.ai.repository;

import com.asc.studycompanion.module.ai.entity.AIUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AIUsageLogRepository extends JpaRepository<AIUsageLog, Long> {

    List<AIUsageLog> findAllByJobIdOrderByCreatedAtDesc(Long jobId);
}
