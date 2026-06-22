package com.complex.platform.evidence;

/** 自动留痕率统计结果：REQ-40 验收口径，默认阈值 80%。 */
public record TraceabilityAutoCaptureRate(
        long totalSteps,
        long autoCapturedSteps,
        double autoCaptureRatePercent,
        int thresholdPercent,
        boolean meetsThreshold
) {
}
