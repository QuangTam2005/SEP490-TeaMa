package com.asc.studycompanion.module.ai.config;

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

    private int maxSources = 3;
    private int maxContextTokens = 2048;
    private int searchConnectTimeoutMs = 5000;
    private int searchReadTimeoutMs = 10000;
    private int llmConnectTimeoutMs = 5000;
    private int llmReadTimeoutMs = 25000;
    private CkeyProperties ckey = new CkeyProperties();
    private BraveProperties brave = new BraveProperties();

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
