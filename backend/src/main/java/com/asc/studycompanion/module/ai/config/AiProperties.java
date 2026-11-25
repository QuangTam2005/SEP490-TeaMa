package com.asc.studycompanion.module.ai.config;

import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * External AI & Search provider properties bound from application.yml (never exposed to clients).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    public static final String DEFAULT_TEXTBOOK_SERIES = "Kết nối tri thức với cuộc sống";
    public static final String ENGLISH_TEXTBOOK_SERIES = "Global Success";
    public static final String DEFAULT_TEXTBOOK_EDITION = "GDPT 2018";

    private int maxSources = 2;
    private int maxContextTokens = 2250;
    private String textbookSeries = DEFAULT_TEXTBOOK_SERIES;
    private String englishTextbookSeries = ENGLISH_TEXTBOOK_SERIES;
    private String textbookEdition = DEFAULT_TEXTBOOK_EDITION;
    private int searchConnectTimeoutMs = 5000;
    private int searchReadTimeoutMs = 10000;
    private int llmConnectTimeoutMs = 5000;
    private int llmReadTimeoutMs = 25000;
    private CkeyProperties ckey = new CkeyProperties();
    private BraveProperties brave = new BraveProperties();

    public String resolveTextbookSeries(SubjectCode subjectCode) {
        if (subjectCode == SubjectCode.ENG) {
            return englishTextbookSeries != null && !englishTextbookSeries.isBlank()
                    ? englishTextbookSeries.trim()
                    : ENGLISH_TEXTBOOK_SERIES;
        }
        return textbookSeries != null && !textbookSeries.isBlank()
                ? textbookSeries.trim()
                : DEFAULT_TEXTBOOK_SERIES;
    }

    public String resolveTextbookEdition() {
        return textbookEdition != null && !textbookEdition.isBlank()
                ? textbookEdition.trim()
                : DEFAULT_TEXTBOOK_EDITION;
    }

    @Getter
    @Setter
    public static class CkeyProperties {
        private String baseUrl = "";
        private String apiKey = "";
        private String model = "mimo-v2-flash";
    }

    @Getter
    @Setter
    public static class BraveProperties {
        private String baseUrl = "https://api.search.brave.com/res/v1/web/search";
        private String apiKey = "";
    }
}
