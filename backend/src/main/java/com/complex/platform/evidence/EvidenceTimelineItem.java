package com.complex.platform.evidence;

import java.time.Instant;

public record EvidenceTimelineItem(
        String itemType,
        String referenceId,
        String eventName,
        String result,
        String summary,
        Instant occurredAt
) {
}
