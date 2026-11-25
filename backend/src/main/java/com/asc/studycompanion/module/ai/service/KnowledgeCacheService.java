package com.asc.studycompanion.module.ai.service;

import com.asc.studycompanion.module.ai.entity.KnowledgeCache;
import com.asc.studycompanion.module.ai.provider.SearchSnippet;
import com.asc.studycompanion.module.ai.repository.KnowledgeCacheRepository;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KnowledgeCacheService {

    private static final Duration DEFAULT_TTL = Duration.ofDays(7);
    private static final int MAX_NORMALIZED_TITLE_LEN = 180;

    private final KnowledgeCacheRepository knowledgeCacheRepository;

    @Transactional(readOnly = true)
    public Optional<List<SearchSnippet>> lookup(int grade, SubjectCode subjectCode, String lessonTitle) {
        String key = buildCacheKey(grade, subjectCode, lessonTitle);
        Instant now = Instant.now();

        return knowledgeCacheRepository.findByCacheKey(key)
                .filter(entry -> Boolean.TRUE.equals(entry.getCacheAllowed()))
                .filter(entry -> entry.getExpiresAt() != null && entry.getExpiresAt().isAfter(now))
                .filter(entry -> entry.getSourcesJson() != null && !entry.getSourcesJson().isEmpty())
                .map(KnowledgeCache::getSourcesJson);
    }

    @Transactional
    public void store(
            int grade,
            SubjectCode subjectCode,
            String lessonTitle,
            List<SearchSnippet> snippets,
            boolean cacheAllowed
    ) {
        if (!cacheAllowed || snippets == null || snippets.isEmpty()) {
            return;
        }
        String normalizedTitle = normalizeLessonTitle(lessonTitle);
        if (normalizedTitle.isBlank()) {
            return;
        }

        String key = buildCacheKey(grade, subjectCode, lessonTitle);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(DEFAULT_TTL);

        KnowledgeCache entity = knowledgeCacheRepository.findByCacheKey(key)
                .orElseGet(() -> KnowledgeCache.builder()
                        .cacheKey(key)
                        .grade(grade)
                        .subjectCode(subjectCode.name())
                        .normalizedLessonTitle(normalizedTitle)
                        .policyVersion(KnowledgeCache.CURRENT_POLICY_VERSION)
                        .build());

        entity.setSourcesJson(List.copyOf(snippets));
        entity.setCacheAllowed(true);
        entity.setExpiresAt(expiresAt);
        knowledgeCacheRepository.save(entity);
    }

    public String buildCacheKey(int grade, SubjectCode subjectCode, String lessonTitle) {
        String normalized = normalizeLessonTitle(lessonTitle);
        return KnowledgeCache.CURRENT_POLICY_VERSION + ":" + grade + ":" + subjectCode.name() + ":" + normalized;
    }

    public String normalizeLessonTitle(String lessonTitle) {
        if (lessonTitle == null) {
            return "";
        }
        String cleaned = lessonTitle.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return cleaned.length() <= MAX_NORMALIZED_TITLE_LEN
                ? cleaned
                : cleaned.substring(0, MAX_NORMALIZED_TITLE_LEN);
    }
}
