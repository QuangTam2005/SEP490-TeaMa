package com.asc.studycompanion.module.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiClientConfig {

    @Bean(name = "braveRestClient")
    public RestClient braveRestClient(RestClient.Builder builder, AiProperties aiProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(aiProperties.getSearchConnectTimeoutMs());
        factory.setReadTimeout(aiProperties.getSearchReadTimeoutMs());
        return builder.requestFactory(factory).build();
    }

    @Bean(name = "ckeyRestClient")
    public RestClient ckeyRestClient(RestClient.Builder builder, AiProperties aiProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(aiProperties.getLlmConnectTimeoutMs());
        factory.setReadTimeout(aiProperties.getLlmReadTimeoutMs());
        return builder.requestFactory(factory).build();
    }
}
