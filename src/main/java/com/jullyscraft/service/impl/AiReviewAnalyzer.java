package com.jullyscraft.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewAnalyzer {

    private final GeminiService geminiService;
    private final ObjectMapper  objectMapper;

    @Async
    public ReviewAnalysis analyzeReview(String title, String body, int rating) {
        String prompt = """
            Analyze this product review and respond with JSON only:
            
            Title: %s
            Body: %s
            Rating: %d/5
            
            Respond with this exact JSON:
            {
              "sentiment": "POSITIVE" or "NEUTRAL" or "NEGATIVE",
              "isSpam": true or false,
              "spamReason": "reason if spam, else null",
              "qualityScore": number 1-10
            }
            """.formatted(title, body, rating);

        try {
            String response = geminiService.generateJson(prompt);
            if (response == null) return ReviewAnalysis.defaultResult();

            // Strip markdown if present
            response = response
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            JsonNode node = objectMapper.readTree(response);
            return ReviewAnalysis.builder()
                    .sentiment(node.path("sentiment").asText("NEUTRAL"))
                    .isSpam(node.path("isSpam").asBoolean(false))
                    .spamReason(node.path("spamReason").asText(null))
                    .qualityScore(node.path("qualityScore").asInt(5))
                    .build();

        } catch (Exception e) {
            log.error("Review analysis failed: {}", e.getMessage());
            return ReviewAnalysis.defaultResult();
        }
    }

    // Inner result class
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ReviewAnalysis {
        private String  sentiment;
        private boolean isSpam;
        private String  spamReason;
        private int     qualityScore;

        public static ReviewAnalysis defaultResult() {
            return ReviewAnalysis.builder()
                    .sentiment("NEUTRAL")
                    .isSpam(false)
                    .qualityScore(5)
                    .build();
        }
    }
}