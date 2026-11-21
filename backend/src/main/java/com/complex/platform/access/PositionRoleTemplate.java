package com.complex.platform.access;

// 文件版本：V0.02

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
        String deleteReason,
        // 第十八批 BY 段第 387 条 387-R2（第十八批裁决 81「岗位角色模板补存理由」）：存档人（V386，存档时已写）与存档理由（V740）带出。
        String archivedBy,
        String archiveReason
) {
    /** 387-R2：存档人、理由之前的签名，既有构造点零改动，两项取空。 */
    public PositionRoleTemplate(String id, String positionCode, String staffLevelId, String roleId, String templateId,
                                String organizationId, String status, String createdBy, Instant createdAt, Instant updatedAt,
                                Instant archivedAt, Instant deletedAt, String deleteReviewId, String deleteReason) {
        this(id, positionCode, staffLevelId, roleId, templateId, organizationId, status, createdBy, createdAt, updatedAt,
                archivedAt, deletedAt, deleteReviewId, deleteReason, null, null);
    }

    public PositionRoleTemplate(String id, String positionCode, String staffLevelId, String roleId,
                                String templateId, String organizationId, String status, String createdBy,
                                Instant createdAt, Instant updatedAt) {
        this(id, positionCode, staffLevelId, roleId, templateId, organizationId, status, createdBy,
                createdAt, updatedAt, null, null, null, null);
    }

    PositionRoleTemplate withLifecycle(Instant archived, Instant deleted, String reviewId, String reason, Instant now) {
        return new PositionRoleTemplate(id, positionCode, staffLevelId, roleId, templateId, organizationId,
                "inactive", createdBy, createdAt, now, archived, deleted, reviewId, reason,
                archived == null ? null : archivedBy, archived == null ? null : archiveReason);
    }

    /** 387-R2：存档时带上所填理由（落库见 savePositionTemplateLifecycle）。 */
    PositionRoleTemplate withArchiveReason(String archiveReason) {
        return new PositionRoleTemplate(id, positionCode, staffLevelId, roleId, templateId, organizationId, status, createdBy,
                createdAt, updatedAt, archivedAt, deletedAt, deleteReviewId, deleteReason, archivedBy, archiveReason);
    }
}
