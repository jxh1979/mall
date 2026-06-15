package com.complex.platform.amusement;

import java.time.Instant;
import java.util.List;

public record AmusementSessionRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String projectId,
        Instant startTime,
        Instant endTime,
        Integer capacity,
        String status
) {
}
