package com.complex.platform.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class OpenAiCompatibleLocalLlmClient implements LocalLlmClient {

    static final String T3D_LOCAL_LLM_ADAPTER_READY = "T3D_LOCAL_LLM_ADAPTER_READY";
    static final String LOOPBACK_ONLY_LLM = "LOOPBACK_ONLY_LLM";
    static final String AI_OUTPUT_NO_AUTO_DECISION = "AI_OUTPUT_NO_AUTO_DECISION";

    private final boolean enabled;
    private final URI baseUri;
    private final String model;
    private final Duration timeout;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    OpenAiCompatibleLocalLlmClient(
            ObjectMapper objectMapper,
            @Value("${complex.analytics.local-llm.enabled:false}") boolean enabled,
            @Value("${complex.analytics.local-llm.base-url:http://127.0.0.1:1234/v1}") String baseUrl,
            @Value("${complex.analytics.local-llm.model:local-model}") String model,
            @Value("${complex.analytics.local-llm.timeout:PT8S}") Duration timeout
    ) {
        this.enabled = enabled;
        this.baseUri = URI.create(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl);
        this.model = model;
        this.timeout = timeout;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public LocalLlmSuggestion suggestQuality(String sourceType, String sourceId, String redactedText) {
        if (!enabled) {
            return LocalLlmSuggestion.unavailable("disabled", model);
        }
        if (!isLoopback(baseUri)) {
            return LocalLlmSuggestion.failed("non_loopback_base_url", model);
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "temperature", 0.1,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt()),
                            Map.of("role", "user", "content", redactedText)
                    )
            ));
            HttpRequest request = HttpRequest.newBuilder(chatCompletionsUri())
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return LocalLlmSuggestion.failed("http_" + response.statusCode(), model);
            }
            return parseSuggestion(response.body());
        } catch (IOException exception) {
            return LocalLlmSuggestion.failed("io_error", model);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return LocalLlmSuggestion.failed("interrupted", model);
        } catch (RuntimeException exception) {
            return LocalLlmSuggestion.failed("invalid_response", model);
        }
    }

    private URI chatCompletionsUri() {
        String base = baseUri.toString();
        return URI.create(base + "/chat/completions");
    }

    private boolean isLoopback(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            return false;
        }
        String host = uri.getHost();
        if (host == null) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return "127.0.0.1".equals(normalized) || "localhost".equals(normalized) || "::1".equals(normalized);
    }

    private String systemPrompt() {
        return "Return JSON only with fields quality_score, risk_level, confidence_score, summary, finding_count. "
                + "Do not decide payment, refund, verification, punishment, commission, contract, inventory, or device state.";
    }

    private LocalLlmSuggestion parseSuggestion(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        String content = root.path("choices").path(0).path("message").path("content").asText("").trim();
        if (content.startsWith("```") && content.endsWith("```")) {
            int firstLineEnd = content.indexOf('\n');
            int lastFence = content.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                content = content.substring(firstLineEnd + 1, lastFence).trim();
            }
        }
        JsonNode suggestion = objectMapper.readTree(content);
        return LocalLlmSuggestion.completed(
                "local_llm",
                model,
                suggestion.path("quality_score").asInt(0),
                suggestion.path("risk_level").asText("suspicious"),
                new BigDecimal(suggestion.path("confidence_score").asText("0")),
                suggestion.path("summary").asText("local LLM response requires manual review"),
                suggestion.path("finding_count").asInt(1)
        );
    }
}
