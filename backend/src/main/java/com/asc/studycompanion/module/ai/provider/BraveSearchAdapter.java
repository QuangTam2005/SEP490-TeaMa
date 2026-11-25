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
import java.util.List;
import java.util.Locale;

/**
 * Brave Search adapter targeting Vietnamese "Kết nối tri thức với cuộc sống" textbooks and Wikipedia,
 * capping sources to 2-3 compact snippets to conserve LLM tokens.
 */
@Slf4j
@Service
public class BraveSearchAdapter implements SearchProvider {

    private static final int HARD_SOURCE_CAP = 3;
    private static final int MAX_SNIPPET_CHARS = 350;

    private final RestClient restClient;
    private final AiProperties aiProperties;

    public BraveSearchAdapter(
            @Qualifier("braveRestClient") RestClient restClient,
            AiProperties aiProperties
    ) {
        this.restClient = restClient;
        this.aiProperties = aiProperties;
    }

    @Override
    public List<SearchSnippet> search(SearchQuery query) {
        String apiKey = aiProperties.getBrave().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE, "Brave Search API key is not configured");
        }

        int maxSources = Math.max(1, Math.min(aiProperties.getMaxSources(), HARD_SOURCE_CAP));
        String searchText = buildKnttWikipediaQuery(query);

        URI uri = UriComponentsBuilder.fromUriString(aiProperties.getBrave().getBaseUrl())
                .queryParam("q", searchText)
                .queryParam("count", maxSources + 2)
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
            log.warn("Brave Search request failed: {}", ex.getMessage());
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
            if (snippets.size() >= maxSources) {
                break;
            }
        }
        return snippets;
    }

    public String buildKnttWikipediaQuery(SearchQuery query) {
        String subject = query.subjectName() != null ? query.subjectName().trim() : "";
        String lesson = query.lessonTitle() != null ? query.lessonTitle().trim() : "";
        return String.format(
                "Lớp %d %s %s \"Kết nối tri thức với cuộc sống\" OR site:vi.wikipedia.org",
                query.grade(),
                subject,
                lesson
        ).trim();
    }

    private static int sourcePriorityScore(BraveWebResult result) {
        String combined = (safeLower(result.title()) + " " + safeLower(result.url()) + " " + safeLower(result.description()));
        int score = 0;
        if (combined.contains("kết nối tri thức") || combined.contains("ket-noi-tri-thuc")) {
            score += 3;
        }
        if (combined.contains("wikipedia.org")) {
            score += 2;
        }
        return score;
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
