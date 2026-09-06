package com.complex.platform.access;

// 文件版本：V0.01

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
        Instant updatedAt,
        Instant archivedAt,
        Instant deletedAt,
        String deleteReviewId,
        String deleteReason
) {
    public PositionRoleTemplate(String id, String positionCode, String staffLevelId, String roleId,
                                String templateId, String organizationId, String status, String createdBy,
                                Instant createdAt, Instant updatedAt) {
        this(id, positionCode, staffLevelId, roleId, templateId, organizationId, status, createdBy,
                createdAt, updatedAt, null, null, null, null);
    }

    PositionRoleTemplate withLifecycle(Instant archived, Instant deleted, String reviewId, String reason, Instant now) {
        return new PositionRoleTemplate(id, positionCode, staffLevelId, roleId, templateId, organizationId,
                "inactive", createdBy, createdAt, now, archived, deleted, reviewId, reason);
    }
}
