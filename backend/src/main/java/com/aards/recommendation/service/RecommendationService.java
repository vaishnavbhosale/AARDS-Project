package com.aards.recommendation.service;

import com.aards.config.GeminiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final GeminiClient geminiClient;

    public RecommendationService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public List<Map<String, Object>> getRecommendations(Long uploadId) {
        log.info("Recommendation generation started for uploadId={}", uploadId);
        // Business Rules -> draft (Problem, Reason, Recommendation, Priority) -> Gemini rewrite.
        String polished = geminiClient.rewriteProfessionally("Improve continuous assessment for weak subjects.");
        log.info("Recommendation generation completed for uploadId={}", uploadId);
        return List.of(Map.of(
                "problem", "Low pass % in placeholder subject",
                "reason", "Business rule placeholder",
                "recommendation", polished,
                "priority", "MEDIUM"));
    }
}
