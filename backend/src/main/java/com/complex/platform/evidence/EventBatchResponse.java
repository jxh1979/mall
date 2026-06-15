package com.complex.platform.evidence;

import java.time.Instant;

public record EventBatchResponse(
        int accepted,
        Instant receivedAt
) {
}
