package com.complex.platform.amusement;

import java.time.Instant;
import java.util.List;

public record AmusementSafetyActionRecord(
        String id,
        String inspectionId,
        String actionType,
        String actionResult,
        String comment,
        List<String> attachmentIds,
        Instant createdAt
) {
}
