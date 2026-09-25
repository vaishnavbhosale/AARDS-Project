package com.aards.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final RestTemplate restTemplate;

    @Value("${app.gemini.api-key:change-me}")
    private String apiKey;

    @Value("${app.gemini.model:gemini-1.5-flash}")
    private String model;

    @Value("${app.gemini.api-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String apiUrl;

    public GeminiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Placeholder: business decisions stay in-app; Gemini only rewrites recommendations professionally.
     */
    public String rewriteProfessionally(String draftRecommendation) {
        log.info("Gemini rewrite requested, model={}", model);
        // TODO: call Gemini API with apiKey, return polished text.
        // Skeleton returns draft unchanged with professional prefix.
        return "[Professional draft - Gemini integration pending] " + draftRecommendation;
    }
}
