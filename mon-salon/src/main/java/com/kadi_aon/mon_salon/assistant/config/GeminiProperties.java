package com.kadi_aon.mon_salon.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    /**
     * Clé API Google Gemini (ou variable GEMINI_API_KEY)
     */
    private String apiKey;

    /**
     * Modèle Gemini utilisé pour l'interaction
     */
    private String model = "gemini-3.8-flash";

    /**
     * Base URL de l'API Google Gemini
     */
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";

    /**
     * Timeout en millisecondes pour les requêtes Gemini
     */
    private int timeoutMs = 15000;
}
