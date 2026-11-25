package com.asc.studycompanion.module.ai.repository;

import com.asc.studycompanion.module.ai.entity.KnowledgeCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KnowledgeCacheRepository extends JpaRepository<KnowledgeCache, Long> {

    Optional<KnowledgeCache> findByCacheKey(String cacheKey);
}
