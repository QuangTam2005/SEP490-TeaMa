package com.asc.studycompanion.module.ai.provider;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.ai.config.AiProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Brave Search adapter targeting Wikipedia and relevant Vietnamese lesson websites
 * using the lesson title and main submission excerpt, strictly capped at <= 3 Brave requests
 * and <= 3 deduplicated sources per student submission review.
 */
@Slf4j
@Service
public class BraveSearchAdapter implements SearchProvider {

    private static final int HARD_SOURCE_CAP = 3;
    private static final int MAX_BRAVE_REQUESTS = 3;
    private static final int MAX_SNIPPET_CHARS = 350;
    private static final int MAX_CONTENT_KEYWORDS_CHARS = 90;

    private final RestClient restClient;
    private final AiProperties aiProperties;
    private final ThreadLocal<Integer> lastRequestCount = ThreadLocal.withInitial(() -> 0);

    public BraveSearchAdapter(
            @Qualifier("braveRestClient") RestClient restClient,
            AiProperties aiProperties
    ) {
        this.restClient = restClient;
        this.aiProperties = aiProperties;
    }

    /**
     * Returns and resets the number of Brave HTTP requests sent during the most recent search on this thread.
     */
    public int consumeLastRequestCount() {
        int count = lastRequestCount.get();
        lastRequestCount.remove();
        return count;
    }

    @Override
    public List<SearchSnippet> search(SearchQuery query) {
        String apiKey = aiProperties.getBrave().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE, "Brave Search API key is not configured");
        }

        int maxSources = Math.max(1, Math.min(aiProperties.getMaxSources(), HARD_SOURCE_CAP));
        int requestsSent = 0;

        List<SearchSnippet> collected = new ArrayList<>();
        List<SearchSnippet> sparePool = new ArrayList<>();
        Set<String> seenUrls = new LinkedHashSet<>();

        // Request 1: Educational websites / textbooks covering the lesson title
        String lessonWebQuery = buildLessonWebQuery(query);
        if (!lessonWebQuery.isBlank() && requestsSent < MAX_BRAVE_REQUESTS) {
            requestsSent++;
            List<SearchSnippet> webHits = executeBraveRequest(apiKey, lessonWebQuery, maxSources + 2, true);
            int webLimit = Math.max(1, maxSources - 1);
            collectDistinctSnippets(webHits, webLimit, collected, sparePool, seenUrls);
        }

        // Request 2: Wikipedia article matching the lesson topic directly
        String wikiQuery = buildWikipediaQuery(query);
        if (!wikiQuery.isBlank() && requestsSent < MAX_BRAVE_REQUESTS && collected.size() < maxSources) {
            requestsSent++;
            try {
                List<SearchSnippet> wikiHits = executeBraveRequest(apiKey, wikiQuery, 3, false);
                collectDistinctSnippets(wikiHits, maxSources - collected.size(), collected, sparePool, seenUrls);
            } catch (AppException ex) {
                if (collected.isEmpty() && sparePool.isEmpty()) {
                    lastRequestCount.set(requestsSent);
                    throw ex;
                }
                log.warn("Secondary Wikipedia search failed cleanly with {} primary sources: {}",
                        collected.size(), ex.getMessage());
            }
        }

        // Request 3: Main content keywords from the student's submission if < maxSources found
        String coreKeywords = extractCoreContentKeywords(query.submissionContent());
        if (collected.size() < maxSources && !coreKeywords.isBlank() && requestsSent < MAX_BRAVE_REQUESTS) {
            String contentQuery = buildContentExcerptQuery(query, coreKeywords);
            requestsSent++;
            try {
                List<SearchSnippet> contentHits = executeBraveRequest(apiKey, contentQuery, maxSources + 2, false);
                collectDistinctSnippets(contentHits, maxSources - collected.size(), collected, sparePool, seenUrls);
            } catch (AppException ex) {
                if (collected.isEmpty() && sparePool.isEmpty()) {
                    lastRequestCount.set(requestsSent);
                    throw ex;
                }
                log.warn("Content-excerpt fallback search failed cleanly: {}", ex.getMessage());
            }
        }

        // Top up from spare results of Request 1 without spending any extra Brave requests
        for (SearchSnippet spare : sparePool) {
            if (collected.size() >= maxSources) {
                break;
            }
            collected.add(spare);
        }

        lastRequestCount.set(requestsSent);
        return collected;
    }

    public String buildLessonWebQuery(SearchQuery query) {
        String subject = safe(query.subjectName());
        String lesson = safe(query.lessonTitle());
        if (lesson.isBlank()) {
            lesson = extractCoreContentKeywords(query.submissionContent());
        }
        if (lesson.isBlank()) {
            return "";
        }
        return String.format("Lớp %d %s %s", query.grade(), subject, lesson).replaceAll("\\s+", " ").trim();
    }

    public String buildWikipediaQuery(SearchQuery query) {
        String lesson = safe(query.lessonTitle());
        if (lesson.isBlank()) {
            lesson = extractCoreContentKeywords(query.submissionContent());
        }
        if (lesson.isBlank()) {
            return "";
        }
        return ("site:vi.wikipedia.org " + lesson).replaceAll("\\s+", " ").trim();
    }

    public String buildContentExcerptQuery(SearchQuery query, String coreKeywords) {
        String subject = safe(query.subjectName());
        String keywords = safe(coreKeywords);
        if (keywords.isBlank()) {
            return "";
        }
        return String.format("%s Lớp %d %s", subject, query.grade(), keywords).replaceAll("\\s+", " ").trim();
    }

    /**
     * Backward-compatible alias retained for unit checks.
     */
    public String buildKnttWikipediaQuery(SearchQuery query) {
        return buildLessonWebQuery(query);
    }

    public static String extractCoreContentKeywords(String submissionContent) {
        if (submissionContent == null || submissionContent.isBlank()) {
            return "";
        }
        String sanitized = submissionContent
                .replaceAll("https?://\\S+", " ")
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("[^\\p{L}\\p{N}\\s,.;:-]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (sanitized.isBlank()) {
            return "";
        }

        // Focus on first meaningful sentence / clause to reflect core lesson topic concisely
        int sentenceEnd = -1;
        for (int i = 0; i < Math.min(sanitized.length(), MAX_CONTENT_KEYWORDS_CHARS + 30); i++) {
            char c = sanitized.charAt(i);
            if ((c == '.' || c == ';' || c == '\n') && i >= 25) {
                sentenceEnd = i;
                break;
            }
        }
        String candidate = sentenceEnd > 0 ? sanitized.substring(0, sentenceEnd).trim() : sanitized;
        if (candidate.length() <= MAX_CONTENT_KEYWORDS_CHARS) {
            return candidate;
        }
        int lastSpace = candidate.lastIndexOf(' ', MAX_CONTENT_KEYWORDS_CHARS);
        return (lastSpace > 25 ? candidate.substring(0, lastSpace) : candidate.substring(0, MAX_CONTENT_KEYWORDS_CHARS)).trim();
    }

    private List<SearchSnippet> executeBraveRequest(
            String apiKey,
            String searchText,
            int count,
            boolean failOnException
    ) {
        URI uri = UriComponentsBuilder.fromUriString(aiProperties.getBrave().getBaseUrl())
                .queryParam("q", searchText)
                .queryParam("count", count)
                .queryParam("country", "ALL")
                .queryParam("search_lang", "vi")
                .build()
                .encode()
                .toUri();

        BraveApiResponse body;
        try {
            body = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Subscription-Token", apiKey.trim())
                    .retrieve()
                    .body(BraveApiResponse.class);
        } catch (RestClientException ex) {
            log.warn("Brave Search request failed for query [{}]: {}", searchText, ex.getMessage());
            if (failOnException) {
                throw new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE, "Không thể kết nối công cụ tìm kiếm");
            }
            throw new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE, "Không thể kết nối công cụ tìm kiếm");
        }

        if (body == null || body.web() == null || body.web().results() == null) {
            return List.of();
        }

        Instant retrievedAt = Instant.now();
        List<BraveWebResult> sorted = new ArrayList<>(body.web().results());
        sorted.sort(Comparator.comparingInt(BraveSearchAdapter::sourcePriorityScore).reversed());

        List<SearchSnippet> snippets = new ArrayList<>();
        for (BraveWebResult result : sorted) {
            if (result.url() == null || result.url().isBlank() || result.title() == null || result.title().isBlank()) {
                continue;
            }
            snippets.add(new SearchSnippet(
                    truncate(result.title().trim(), 255),
                    truncate(result.url().trim(), 1000),
                    truncate(cleanText(result.description()), MAX_SNIPPET_CHARS),
                    retrievedAt
            ));
        }
        return snippets;
    }

    private static void collectDistinctSnippets(
            List<SearchSnippet> candidates,
            int maxAdd,
            List<SearchSnippet> collected,
            List<SearchSnippet> sparePool,
            Set<String> seenUrls
    ) {
        int added = 0;
        for (SearchSnippet snippet : candidates) {
            String normalizedUrl = snippet.url().trim().toLowerCase(Locale.ROOT);
            if (!seenUrls.add(normalizedUrl)) {
                continue;
            }
            if (added < maxAdd) {
                collected.add(snippet);
                added++;
            } else {
                sparePool.add(snippet);
            }
        }
    }

    private static int sourcePriorityScore(BraveWebResult result) {
        String url = safeLower(result.url());
        String combined = safeLower(result.title()) + " " + url + " " + safeLower(result.description());
        int score = 0;
        if (url.contains("vi.wikipedia.org") || url.contains("wikipedia.org")) {
            score += 4;
        }
        if (combined.contains("kết nối tri thức") || combined.contains("ket-noi-tri-thuc")) {
            score += 3;
        }
        if (url.contains("vietjack") || url.contains("loigiaihay") || url.contains("olm.vn")
                || url.contains("hoc247") || url.contains("tech12h") || url.contains("vuihoc")) {
            score += 2;
        }
        return score;
    }

    private static String safe(String input) {
        return input == null ? "" : input.trim();
    }

    private static String safeLower(String input) {
        return input == null ? "" : input.toLowerCase(Locale.ROOT);
    }

    private static String cleanText(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BraveApiResponse(@JsonProperty("web") BraveWebResults web) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BraveWebResults(@JsonProperty("results") List<BraveWebResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BraveWebResult(
            @JsonProperty("title") String title,
            @JsonProperty("url") String url,
            @JsonProperty("description") String description
    ) {
    }
}
