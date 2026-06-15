package com.complex.platform.analytics;

import java.math.BigDecimal;

public interface LocalLlmClient {

    LocalLlmSuggestion suggestQuality(String sourceType, String sourceId, String redactedText);
}

record LocalLlmSuggestion(
        String modelProvider,
        String modelVersion,
        String modelStatus,
        int qualityScore,
        String riskLevel,
        BigDecimal confidenceScore,
        String summary,
        int findingCount
) {
    static LocalLlmSuggestion completed(
            String modelProvider,
            String modelVersion,
            int qualityScore,
            String riskLevel,
            BigDecimal confidenceScore,
            String summary,
            int findingCount
    ) {
        return new LocalLlmSuggestion(
                modelProvider,
                modelVersion,
                "completed",
                qualityScore,
                riskLevel,
                confidenceScore,
                summary,
                findingCount
        );
    }

    static LocalLlmSuggestion unavailable(String reason, String modelVersion) {
        return new LocalLlmSuggestion(
                "local_llm",
                modelVersion,
                "unavailable",
                0,
                "suspicious",
                BigDecimal.ZERO,
                "local LLM unavailable: " + reason + "; manual review required",
                1
        );
    }

    static LocalLlmSuggestion failed(String reason, String modelVersion) {
        return new LocalLlmSuggestion(
                "local_llm",
                modelVersion,
                "failed",
                0,
                "suspicious",
                BigDecimal.ZERO,
                "local LLM failed: " + reason + "; manual review required",
                1
        );
    }
}
