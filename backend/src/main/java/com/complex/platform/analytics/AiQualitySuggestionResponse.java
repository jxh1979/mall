package com.complex.platform.analytics;

import com.complex.platform.analytics.AnalyticsRecords.AiQualityResultRecord;
import com.complex.platform.analytics.AnalyticsRecords.AiQualityTaskRecord;

public record AiQualitySuggestionResponse(
        AiQualityTaskRecord task,
        AiQualityResultRecord result,
        String modelStatus,
        boolean redactionApplied,
        boolean manualReviewRequired
) {
}
