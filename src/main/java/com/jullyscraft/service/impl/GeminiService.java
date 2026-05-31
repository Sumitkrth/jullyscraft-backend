package com.jullyscraft.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.model:gemini-1.5-flash}")
    private String model;

    @Value("${app.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public String generate(String prompt) {
        try {
            String url = baseUrl + "/models/" + model + ":generateContent?key=" + apiKey;

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "temperature",     0.7,
                            "maxOutputTokens", 1024,
                            "topP",            0.8
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, request, String.class
            );

            // Parse response
            JsonNode root = objectMapper.readTree(response.getBody());
            return root
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

        } catch (Exception e) {
            log.error("Gemini API error: {}", e.getMessage());
            return null;
        }
    }

    // JSON mode — returns clean JSON string
    public String generateJson(String prompt) {
        String jsonPrompt = prompt +
                "\n\nIMPORTANT: Respond with valid JSON only. No markdown, no backticks, no explanation.";
        return generate(jsonPrompt);
    }
}