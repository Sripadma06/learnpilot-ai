package com.learnpilot.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Minimal Anthropic Messages API client. Optional: when ANTHROPIC_API_KEY is not set, enabled() is false and
 * callers fall back to the offline generators, so the app works with no key.
 */
@Component
public class LlmClient {
    private final String apiKey;
    private final String model;
    private final String geminiKey;
    private final String geminiModel;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public LlmClient(@Value("${app.llm.api-key:}") String apiKey,
                     @Value("${app.llm.model:claude-sonnet-5-5}") String model,
                     @Value("${app.llm.gemini-key:}") String geminiKey,
                     @Value("${app.llm.gemini-model:gemini-2.5-flash}") String geminiModel) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.geminiKey = geminiKey == null ? "" : geminiKey.trim();
        this.geminiModel = geminiModel;
    }

    public boolean enabled() { return !apiKey.isEmpty() || !geminiKey.isEmpty(); }

    public String complete(String system, String user, int maxTokens) throws IOException, InterruptedException {
        return !apiKey.isEmpty() ? completeAnthropic(system, user, maxTokens) : completeGemini(system, user, maxTokens);
    }

    /** Google Gemini (has a free tier via Google AI Studio). */
    private String completeGemini(String system, String user, int maxTokens) throws IOException, InterruptedException {
        Map<String, Object> body = Map.of(
            "systemInstruction", Map.of("parts", List.of(Map.of("text", system))),
            "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", user)))),
            "generationConfig", Map.of("maxOutputTokens", maxTokens, "temperature", 0.4));
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                "https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent"))
            .timeout(Duration.ofSeconds(90))
            .header("x-goog-api-key", geminiKey)
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
            .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Gemini request failed with status " + response.statusCode());
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : mapper.readTree(response.body()).path("candidates").path(0).path("content").path("parts")) {
            text.append(part.path("text").asText(""));
        }
        return text.toString();
    }

    private String completeAnthropic(String system, String user, int maxTokens) throws IOException, InterruptedException {
        Map<String, Object> body = Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "system", system,
            "messages", List.of(Map.of("role", "user", "content", user)));
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
            .timeout(Duration.ofSeconds(90))
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
            .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("LLM request failed with status " + response.statusCode());
        }
        JsonNode content = mapper.readTree(response.body()).path("content");
        StringBuilder text = new StringBuilder();
        for (JsonNode block : content) {
            if ("text".equals(block.path("type").asText())) text.append(block.path("text").asText());
        }
        return text.toString();
    }
}
