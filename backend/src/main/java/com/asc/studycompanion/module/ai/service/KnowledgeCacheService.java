package com.asc.studycompanion.module.ai.service;

import com.asc.studycompanion.module.ai.config.AiProperties;
import com.asc.studycompanion.module.ai.entity.KnowledgeCache;
import com.asc.studycompanion.module.ai.provider.SearchSnippet;
import com.asc.studycompanion.module.ai.repository.KnowledgeCacheRepository;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KnowledgeCacheService {

    private static final Duration DEFAULT_TTL = Duration.ofDays(7);
    private static final int MAX_NORMALIZED_TITLE_LEN = 120;
    private static final int MAX_SLUG_LEN = 32;

    private final KnowledgeCacheRepository knowledgeCacheRepository;

    @Transactional(readOnly = true)
    public Optional<List<SearchSnippet>> lookup(int grade, SubjectCode subjectCode, String lessonTitle) {
        return lookup(
                grade,
                subjectCode,
                lessonTitle,
                defaultSeries(subjectCode),
                AiProperties.DEFAULT_TEXTBOOK_EDITION
        );
    }

    @Transactional(readOnly = true)
    public Optional<List<SearchSnippet>> lookup(
            int grade,
            SubjectCode subjectCode,
            String lessonTitle,
            String textbookSeries,
            String textbookEdition
    ) {
        String key = buildCacheKey(grade, subjectCode, lessonTitle, textbookSeries, textbookEdition);
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
        store(
                grade,
                subjectCode,
                lessonTitle,
                defaultSeries(subjectCode),
                AiProperties.DEFAULT_TEXTBOOK_EDITION,
                snippets,
                cacheAllowed
        );
    }

    @Transactional
    public void store(
            int grade,
            SubjectCode subjectCode,
            String lessonTitle,
            String textbookSeries,
            String textbookEdition,
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

        String key = buildCacheKey(grade, subjectCode, lessonTitle, textbookSeries, textbookEdition);
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
        return buildCacheKey(
                grade,
                subjectCode,
                lessonTitle,
                defaultSeries(subjectCode),
                AiProperties.DEFAULT_TEXTBOOK_EDITION
        );
    }

    public String buildCacheKey(
            int grade,
            SubjectCode subjectCode,
            String lessonTitle,
            String textbookSeries,
            String textbookEdition
    ) {
        String normalized = normalizeLessonTitle(lessonTitle);
        String seriesSlug = slugifyTextbookSeries(
                textbookSeries != null && !textbookSeries.isBlank() ? textbookSeries : defaultSeries(subjectCode)
        );
        String editionSlug = slugifyToken(
                textbookEdition != null && !textbookEdition.isBlank()
                        ? textbookEdition
                        : AiProperties.DEFAULT_TEXTBOOK_EDITION
        );
        return KnowledgeCache.CURRENT_POLICY_VERSION
                + ":" + seriesSlug
                + ":" + editionSlug
                + ":" + grade
                + ":" + subjectCode.name()
                + ":" + normalized;
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

    private static String slugifyTextbookSeries(String series) {
        String lower = series.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("kết nối tri thức") || lower.contains("ket noi tri thuc")) {
            return "kntt";
        }
        if (lower.contains("global success")) {
            return "global-success";
        }
        if (lower.contains("chân trời sáng tạo") || lower.contains("chan troi sang tao")) {
            return "ctst";
        }
        if (lower.contains("cánh diều") || lower.contains("canh dieu")) {
            return "canh-dieu";
        }
        return slugifyToken(series);
    }

    private static String slugifyToken(String raw) {
        String ascii = Normalizer.normalize(raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (ascii.isBlank()) {
            return "default";
        }
        return ascii.length() <= MAX_SLUG_LEN ? ascii : ascii.substring(0, MAX_SLUG_LEN);
    }

    private static String defaultSeries(SubjectCode code) {
        return code == SubjectCode.ENG
                ? AiProperties.ENGLISH_TEXTBOOK_SERIES
                : AiProperties.DEFAULT_TEXTBOOK_SERIES;
    }
}
