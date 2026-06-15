package com.complex.platform.access;

import java.time.Instant;

public record PermissionTemplate(
        String id,
        String templateCode,
        String templateName,
        String description,
        String templateType,
        String sourceTemplateId,
        String ownerScopeType,
        String ownerScopeId,
        String usageMode,
        Instant validFrom,
        Instant validUntil,
        boolean singleUse,
        String status,
        String reason,
        String approvedBy,
        Instant approvedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
