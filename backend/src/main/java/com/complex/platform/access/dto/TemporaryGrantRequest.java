package com.complex.platform.access.dto;

import java.time.Instant;
import java.util.List;

public record TemporaryGrantRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String targetUserId,
        List<String> permissionCodes,
        Instant validFrom,
        Instant validUntil,
        Boolean singleUse,
        String reason
) {
}
