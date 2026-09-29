package com.project.InboxBuddy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class EmailGenService {

    private final WebClient webClient;
    private final String apikey;
    private final String model;

    public EmailGenService(WebClient.Builder webClientBuilder,
                           @Value("${gemini.api.url}") String baseUrl,
                           @Value("${gemini.api.key}") String geminiApiKey,
                           @Value("${gemini.api.model:gemini-3.5-flash-lite}") String model) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apikey = geminiApiKey;
        this.model = model;
    }

    public String generateEmailReply(EmailRequest emailRequest) {
        if (emailRequest == null || emailRequest.getEmailContent() == null
                || emailRequest.getEmailContent().isBlank()) {
            throw new IllegalArgumentException("emailContent must not be empty");
        }
        if (apikey == null || apikey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured");
        }
        // Build prompt
        String prompt = buildPrompt(emailRequest);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        // Bug #2 Fix: Add .onStatus() handler so 4xx/5xx from Gemini give a meaningful error instead of a raw 500
        ResponseEntity<String> upstreamResponse = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/{model}:generateContent")
                        .build(model))
                .header("x-goog-api-key", apikey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .toEntity(String.class)
                .block();

        if (upstreamResponse == null) {
            throw new RuntimeException("Gemini API returned no HTTP response");
        }

        String response = upstreamResponse.getBody();
        if (upstreamResponse.getStatusCode().isError()) {
            String errorBody = response == null ? "<empty body>" : response;
            String message = "Gemini API error [" + upstreamResponse.getStatusCode() + "]: " + errorBody;
            if (upstreamResponse.getStatusCode().value() == 401 ||
                    upstreamResponse.getStatusCode().value() == 403) {
                message = "Gemini API key was rejected or reported as leaked. " +
                        "Create a new key and replace GEMINI_API_KEY, then restart the server. " +
                        "Details: " + errorBody;
            }
            throw new RuntimeException(message);
        }

        if (response == null || response.isBlank()) {
            throw new RuntimeException("Gemini API returned an empty response with HTTP status " +
                    upstreamResponse.getStatusCode());
        }

        // Extract response
        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) {
        // Bug #3 Fix: Null-safe traversal — check candidates array before calling .get(0)
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);

            JsonNode candidates = root.path("candidates");
            if (candidates.isMissingNode() || !candidates.isArray() || candidates.isEmpty()) {
                String feedback = root.path("promptFeedback").path("blockReason").asText("");
                if (!feedback.isEmpty()) {
                    throw new RuntimeException("Gemini blocked the prompt: " + feedback);
                }
                throw new RuntimeException("Gemini returned no candidates. Response: " + response);
            }

            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isMissingNode() || !parts.isArray() || parts.isEmpty()) {
                throw new RuntimeException("No parts found in Gemini response candidate.");
            }

            String text = parts.get(0).path("text").asText("").trim();
            if (text.isEmpty()) {
                throw new RuntimeException("Gemini response did not contain reply text.");
            }
            return text;

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse Gemini API response: " + response, e);
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional email reply for the following email. ");
        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("Use a ").append(emailRequest.getTone()).append(" tone. ");
        }
        prompt.append("Original Email: ").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}
