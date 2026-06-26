package com.complex.platform.access;

import java.time.Instant;

public record PositionRoleTemplate(
        String id,
        String positionCode,
        String staffLevelId,
        String roleId,
        String templateId,
        String organizationId,
        String status,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
