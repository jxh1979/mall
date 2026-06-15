package com.complex.platform.evidence;

import java.time.Instant;
import java.util.List;

public record EvidenceReviewRecord(
        String id,
        String businessType,
        String businessId,
        String riskLevel,
        String decision,
        String comment,
        List<String> evidenceAttachmentIds,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
