package com.asc.studycompanion.module.ai.provider;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.ai.config.AiProperties;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
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
 * Brave Search adapter (Option B: extra_snippets=true, <= 3 Brave HTTP calls, <= 2 sources, <= 1,000 chars/source)
 * with subject-specific source routing (Global Success for ENG, textbook portals for LIT,
 * official curriculum portals + Wikipedia for conceptual subjects) and non-educational filtering.
 */
@Slf4j
@Service
public class BraveSearchAdapter implements SearchProvider {

    private static final int HARD_SOURCE_CAP = 2;
    private static final int MAX_BRAVE_REQUESTS = 3;
    private static final int MAX_SNIPPET_CHARS = 1000;
    private static final int MAX_CONTENT_KEYWORDS_CHARS = 90;

    private static final List<String> BLOCKED_NON_EDUCATIONAL_HOSTS = List.of(
            "facebook.com",
            "fb.com",
            "tiktok.com",
            "youtube.com",
            "youtu.be",
            "instagram.com",
            "pinterest.",
            "shopee.",
            "lazada.",
            "tiki.vn",
            "threads.net",
            "twitter.com",
            "x.com",
            "wattpad.com",
            "scribd.com",
            // Un-crawlable JS SPAs, admin circulars, and Cloudflare/timeout/anti-bot-blocked sites
            "hanhtrangso.nxbgd.vn",
            "hoc10.vn",
            "moet.gov.vn",
            "olm.vn",
            "hoc247.net",
            "haylamdo.com",
            "thuvienhoclieu.com",
            "hocmai.vn",
            "baitap123.com"
    );

    // Server-rendered Vietnamese K12 curriculum portals verified crawlable (HTTP 200, no Cloudflare JS wall, no SPA shell)
    private static final List<String> HIGH_CRAWL_CURRICULUM_PORTALS = List.of(
            "vietjack.com",
            "khoahoc.vietjack.com",
            "loigiaihay.com",
            "vndoc.com",
            "tech12h.com",
            "vuihoc.vn",
            "doctailieu.com"
    );

    // Secondary verified crawlable Vietnamese subject portals & Wikimedia educational references
    private static final List<String> SECONDARY_EDU_PORTALS = List.of(
            "tailieumoi.vn",
            "quantrimang.com",
            "toanmath.com",
            "monkey.edu.vn",
            "colearn.vn",
            "hocgioi.vn",
            "vi.wikibooks.org",
            "vi.wiktionary.org",
            "taphuan.nxbgd.vn"
    );

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

        // Request 1: Primary curriculum/textbook websites matching grade, subject, series & lesson
        String lessonWebQuery = buildLessonWebQuery(query);
        if (!lessonWebQuery.isBlank() && requestsSent < MAX_BRAVE_REQUESTS) {
            requestsSent++;
            List<SearchSnippet> webHits = executeBraveRequest(apiKey, lessonWebQuery, maxSources + 3, true, query);
            int webLimit = Math.max(1, maxSources - 1);
            collectDistinctSnippets(webHits, webLimit, collected, sparePool, seenUrls);
        }

        // Request 2: Subject-routed secondary source (Global Success for ENG, literary portals for LIT, Wikipedia for others)
        String secondaryQuery = buildSecondarySourceQuery(query);
        if (!secondaryQuery.isBlank() && requestsSent < MAX_BRAVE_REQUESTS && collected.size() < maxSources) {
            requestsSent++;
            try {
                List<SearchSnippet> secondaryHits = executeBraveRequest(apiKey, secondaryQuery, maxSources + 1, false, query);
                collectDistinctSnippets(secondaryHits, maxSources - collected.size(), collected, sparePool, seenUrls);
            } catch (AppException ex) {
                if (collected.isEmpty() && sparePool.isEmpty()) {
                    lastRequestCount.set(requestsSent);
                    throw ex;
                }
                log.warn("Secondary curriculum search failed cleanly with {} primary sources: {}",
                        collected.size(), ex.getMessage());
            }
        }

        // Request 3: Main content keywords from submission excerpt if still < maxSources
        String coreKeywords = extractCoreContentKeywords(query.submissionContent());
        if (collected.size() < maxSources && !coreKeywords.isBlank() && requestsSent < MAX_BRAVE_REQUESTS) {
            String contentQuery = buildContentExcerptQuery(query, coreKeywords);
            requestsSent++;
            try {
                List<SearchSnippet> contentHits = executeBraveRequest(apiKey, contentQuery, maxSources + 2, false, query);
                collectDistinctSnippets(contentHits, maxSources - collected.size(), collected, sparePool, seenUrls);
            } catch (AppException ex) {
                if (collected.isEmpty() && sparePool.isEmpty()) {
                    lastRequestCount.set(requestsSent);
                    throw ex;
                }
                log.warn("Content-excerpt fallback search failed cleanly: {}", ex.getMessage());
            }
        }

        // Top up from spare pool of Request 1 without spending an extra Brave credit
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
        String seriesHint = shortenTextbookSeries(query.resolvedTextbookSeries());
        String subjectFocus = subjectFocusHint(query.subjectCode());
        return String.format("Lớp %d %s %s %s %s", query.grade(), subject, lesson, seriesHint, subjectFocus)
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String buildSecondarySourceQuery(SearchQuery query) {
        SubjectCode code = query.subjectCode();
        String lesson = stripLessonNumberPrefix(safe(query.lessonTitle()));
        if (lesson.isBlank()) {
            lesson = extractCoreContentKeywords(query.submissionContent());
        }
        if (lesson.isBlank()) {
            return "";
        }
        String seriesHint = shortenTextbookSeries(query.resolvedTextbookSeries());
        if (code == SubjectCode.ENG) {
            return String.format("\"%s\" Lớp %d Tiếng Anh %s từ vựng ngữ pháp", seriesHint, query.grade(), lesson)
                    .replaceAll("\\s+", " ")
                    .trim();
        }
        if (code == SubjectCode.LIT) {
            return String.format("Ngữ văn Lớp %d %s %s nội dung nghệ thuật", query.grade(), lesson, seriesHint)
                    .replaceAll("\\s+", " ")
                    .trim();
        }
        return buildWikipediaQuery(query);
    }

    public String buildWikipediaQuery(SearchQuery query) {
        String lesson = stripLessonNumberPrefix(safe(query.lessonTitle()));
        if (lesson.isBlank()) {
            lesson = extractCoreContentKeywords(query.submissionContent());
        }
        if (lesson.isBlank()) {
            return "";
        }
        return ("site:vi.wikipedia.org " + lesson).replaceAll("\\s+", " ").trim();
    }

    private static String shortenTextbookSeries(String series) {
        String s = safe(series);
        if (s.toLowerCase(Locale.ROOT).contains("kết nối tri thức")) {
            return "Kết nối tri thức";
        }
        return s.isBlank() ? "Kết nối tri thức" : s;
    }

    private static String subjectFocusHint(SubjectCode subjectCode) {
        if (subjectCode == null) {
            return "lý thuyết trọng tâm";
        }
        return switch (subjectCode) {
            case ENG -> "từ vựng ngữ pháp";
            case LIT -> "soạn bài tác phẩm";
            case MATH, PHY, CHEM -> "định nghĩa công thức";
            case SCI, BIO, TECH, INFO, HISGEO, HIST, GEO, CIVICS -> "lý thuyết trọng tâm";
        };
    }

    private static String stripLessonNumberPrefix(String rawTitle) {
        if (rawTitle == null || rawTitle.isBlank()) {
            return "";
        }
        String stripped = rawTitle.trim()
                .replaceFirst("^(?iu)(bài|chương|chủ đề|tiết|phần|unit|lesson)\\s+\\d+\\s*[:.\\-–]?\\s*", "");
        return stripped.isBlank() ? rawTitle.trim() : stripped.trim();
    }

    public String buildContentExcerptQuery(SearchQuery query, String coreKeywords) {
        String subject = safe(query.subjectName());
        String keywords = safe(coreKeywords);
        if (keywords.isBlank()) {
            return "";
        }
        String seriesHint = shortenTextbookSeries(query.resolvedTextbookSeries());
        return String.format("%s Lớp %d %s %s", subject, query.grade(), seriesHint, keywords)
                .replaceAll("\\s+", " ")
                .trim();
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
            boolean failOnException,
            SearchQuery query
    ) {
        URI uri = UriComponentsBuilder.fromUriString(aiProperties.getBrave().getBaseUrl())
                .queryParam("q", searchText)
                .queryParam("count", count)
                .queryParam("country", "ALL")
                .queryParam("search_lang", "vi")
                .queryParam("extra_snippets", "true")
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
        sorted.sort(Comparator.comparingInt((BraveWebResult r) -> sourcePriorityScore(r, query)).reversed());

        List<SearchSnippet> snippets = new ArrayList<>();
        for (BraveWebResult result : sorted) {
            if (result.url() == null || result.url().isBlank() || result.title() == null || result.title().isBlank()) {
                continue;
            }
            if (isIrrelevantOrBlockedResult(result, query)) {
                continue;
            }
            String mergedSnippet = mergeSnippets(result.description(), result.extraSnippets());
            if (mergedSnippet.isBlank()) {
                continue;
            }
            snippets.add(new SearchSnippet(
                    truncate(result.title().trim(), 255),
                    truncate(result.url().trim(), 1000),
                    mergedSnippet,
                    retrievedAt
            ));
        }
        return snippets;
    }

    static String mergeSnippets(String description, List<String> extraSnippets) {
        List<String> parts = new ArrayList<>();
        String primary = cleanText(description);
        if (!primary.isBlank()) {
            parts.add(primary);
        }
        if (extraSnippets != null) {
            for (String extra : extraSnippets) {
                String cleaned = cleanText(extra);
                if (cleaned.isBlank() || cleaned.length() < 20) {
                    continue;
                }
                String lower = cleaned.toLowerCase(Locale.ROOT);
                boolean duplicate = parts.stream().anyMatch(existing -> {
                    String existingLower = existing.toLowerCase(Locale.ROOT);
                    return existingLower.contains(lower) || lower.contains(existingLower);
                });
                if (!duplicate) {
                    parts.add(cleaned);
                }
            }
        }
        if (parts.isEmpty()) {
            return "";
        }
        String joined = String.join(" | ", parts);
        if (joined.length() <= MAX_SNIPPET_CHARS) {
            return joined;
        }
        String slice = joined.substring(0, MAX_SNIPPET_CHARS);
        int lastSep = Math.max(slice.lastIndexOf('.'), slice.lastIndexOf(' '));
        if (lastSep >= MAX_SNIPPET_CHARS - 120) {
            return slice.substring(0, lastSep).trim();
        }
        return slice.trim();
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

    private static boolean isIrrelevantOrBlockedResult(BraveWebResult result, SearchQuery query) {
        String url = safeLower(result.url());
        for (String blocked : BLOCKED_NON_EDUCATIONAL_HOSTS) {
            if (url.contains(blocked)) {
                return true;
            }
        }
        if (isMetaPublisherPage(result)) {
            return true;
        }
        // Avoid foreign Wikipedia results for Vietnamese curriculum subjects unless English
        if (query != null && query.subjectCode() != SubjectCode.ENG
                && url.contains("wikipedia.org") && !url.contains("vi.wikipedia.org")) {
            return true;
        }
        return false;
    }

    private static boolean isMetaPublisherPage(BraveWebResult result) {
        String url = safeLower(result.url());
        if (!url.contains("wikipedia.org")) {
            return false;
        }
        String title = safeLower(result.title());
        return title.startsWith("kết nối tri thức với cuộc sống")
                || title.startsWith("nhà xuất bản giáo dục việt nam");
    }

    private static int sourcePriorityScore(BraveWebResult result, SearchQuery query) {
        String url = safeLower(result.url());
        String combined = safeLower(result.title()) + " " + url + " " + safeLower(result.description());
        int score = 0;

        for (String portal : HIGH_CRAWL_CURRICULUM_PORTALS) {
            if (url.contains(portal)) {
                score += 6;
                break;
            }
        }

        if (url.contains("vi.wikipedia.org")) {
            score += (query != null && query.subjectCode() == SubjectCode.ENG) ? 1 : 5;
        }

        if (combined.contains("kết nối tri thức") || combined.contains("ket-noi-tri-thuc")
                || combined.contains("global success") || combined.contains("global-success")) {
            score += 4;
        }

        for (String secondary : SECONDARY_EDU_PORTALS) {
            if (url.contains(secondary)) {
                score += 3;
                break;
            }
        }

        if (query != null) {
            String gradeToken = "lớp " + query.grade();
            String gradeSlug = "lop-" + query.grade();
            if (combined.contains(gradeToken) || url.contains(gradeSlug)) {
                score += 2;
            }
            if (query.subjectCode() == SubjectCode.ENG
                    && (combined.contains("global success") || combined.contains("global-success"))) {
                score += 3;
            }
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
            @JsonProperty("description") String description,
            @JsonProperty("extra_snippets") List<String> extraSnippets
    ) {
    }
}
