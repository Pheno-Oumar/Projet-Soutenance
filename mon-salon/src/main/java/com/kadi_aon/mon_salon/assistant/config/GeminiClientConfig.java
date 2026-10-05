package com.kadi_aon.mon_salon.assistant.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class GeminiClientConfig {

    private final GeminiProperties geminiProperties;

    @Bean
    public RestClient geminiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofMillis(geminiProperties.getTimeoutMs()).toMillis());
        factory.setReadTimeout((int) Duration.ofMillis(geminiProperties.getTimeoutMs()).toMillis());

        return RestClient.builder()
                .baseUrl(geminiProperties.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        return mapper;
    }
}
