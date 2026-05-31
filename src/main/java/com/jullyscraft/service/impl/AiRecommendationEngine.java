package com.jullyscraft.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendationEngine {

    private final GeminiService geminiService;
    private final ObjectMapper  objectMapper;

    public List<Long> rankProducts(
            List<Long> candidateIds,
            String userContext,
            String productContext
    ) {
        if (candidateIds == null || candidateIds.isEmpty()) return candidateIds;

        String prompt = """
            You are a product recommendation engine.
            
            User context: %s
            Product context: %s
            Candidate product IDs: %s
            
            Rank these product IDs by relevance for this user.
            Respond with JSON array only: [id1, id2, id3, ...]
            Keep all IDs, just reorder them.
            """.formatted(userContext, productContext, candidateIds);

        try {
            String response = geminiService.generateJson(prompt);
            if (response == null) return candidateIds;

            response = response
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            JsonNode arr = objectMapper.readTree(response);
            List<Long> ranked = new ArrayList<>();
            for (JsonNode node : arr) {
                ranked.add(node.asLong());
            }
            return ranked.isEmpty() ? candidateIds : ranked;

        } catch (Exception e) {
            log.error("AI ranking failed: {}", e.getMessage());
            return candidateIds; // fallback — return original order
        }
    }
}