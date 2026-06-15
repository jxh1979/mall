package com.complex.platform.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleLocalLlmClientTests {

    @Test
    void disabledClientReturnsManualReviewFallbackWithoutNetworkCall() {
        OpenAiCompatibleLocalLlmClient client = new OpenAiCompatibleLocalLlmClient(
                new ObjectMapper(),
                false,
                "http://127.0.0.1:1234/v1",
                "local-model",
                Duration.ofMillis(50)
        );

        LocalLlmSuggestion suggestion = client.suggestQuality("ticket_call", "call_1", "redacted text");

        assertThat(suggestion.modelStatus()).isEqualTo("unavailable");
        assertThat(suggestion.summary()).contains("disabled");
    }

    @Test
    void enabledClientRejectsNonLoopbackBaseUrlWithoutNetworkCall() {
        OpenAiCompatibleLocalLlmClient client = new OpenAiCompatibleLocalLlmClient(
                new ObjectMapper(),
                true,
                "http://192.168.88.113:1234/v1",
                "local-model",
                Duration.ofMillis(50)
        );

        LocalLlmSuggestion suggestion = client.suggestQuality("ticket_call", "call_1", "redacted text");

        assertThat(suggestion.modelStatus()).isEqualTo("failed");
        assertThat(suggestion.summary()).contains("non_loopback_base_url");
    }

    @Test
    void enabledClientRejectsNonHttpLoopbackBaseUrlWithoutNetworkCall() {
        OpenAiCompatibleLocalLlmClient client = new OpenAiCompatibleLocalLlmClient(
                new ObjectMapper(),
                true,
                "ftp://127.0.0.1:1234/v1",
                "local-model",
                Duration.ofMillis(50)
        );

        LocalLlmSuggestion suggestion = client.suggestQuality("ticket_call", "call_1", "redacted text");

        assertThat(suggestion.modelStatus()).isEqualTo("failed");
        assertThat(suggestion.summary()).contains("non_loopback_base_url");
    }
}
