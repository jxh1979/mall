package com.complex.platform.bar;

import java.time.Instant;

/**
 * 酒吧门店主数据（V263）：独立门店实体，挂项目（综合体），可选绑经营商户与所在空间。
 * 各处的 bar_id 从此表下拉选（值=id，显示 barName）。存档=软下线可恢复；删除=硬删走审批（照 V262 范式）。
 */
public record BarVenueRecord(
        String id,
        String projectId,
        String merchantId,
        String spaceId,
        String barName,
        String barCode,
        Long minimumSpendCent,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt,
        String archivedBy,
        String deleteStatus,
        String deleteRequestedBy,
        Instant deleteRequestedAt,
        String deleteReason
) {
    public BarVenueRecord withArchive(Instant archivedAt, String archivedBy, Instant updatedAt) {
        return new BarVenueRecord(id, projectId, merchantId, spaceId, barName, barCode, minimumSpendCent, status,
                createdAt, updatedAt, archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt,
                deleteReason);
    }

    public BarVenueRecord withDelete(String deleteStatus, String deleteRequestedBy,
            Instant deleteRequestedAt, String deleteReason, Instant updatedAt) {
        return new BarVenueRecord(id, projectId, merchantId, spaceId, barName, barCode, minimumSpendCent, status,
                createdAt, updatedAt, archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt,
                deleteReason);
    }
}
