package com.complex.platform.access;

import java.time.Instant;

public record PermissionTemplateApplication(
        String id,
        String templateId,
        String targetType,
        String targetId,
        Instant validFrom,
        Instant validUntil,
        boolean singleUse,
        boolean consumed,
        String status,
        String appliedBy,
        Instant appliedAt,
        String revokedBy,
        Instant revokedAt
) {
}
