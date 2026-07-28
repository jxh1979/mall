package com.complex.platform.amusement;

import java.time.Instant;

/**
 * 游乐项目可用权益类型绑定记录（amusement_project_entitlements）。
 *
 * <p>V341（v7 审计 R-12）软删范式字段族，与 V255/V260/V271/V272 完全对齐：
 * 存档轴 archivedAt/archivedBy/archiveReason；删除轴 deleteStatus(none|pending|approved)
 * + deleteRequestedBy/deleteRequestedAt/deleteReason。deleteStatus='approved' 为墓碑（行留库、读取路径过滤）。
 */
public record AmusementProjectEntitlementRecord(
        String id,
        String projectId,
        String entitlementType,
        Instant createdAt,
        Instant archivedAt,
        String archivedBy,
        String archiveReason,
        String deleteStatus,
        String deleteRequestedBy,
        Instant deleteRequestedAt,
        String deleteReason
) {

    /** 向后兼容构造器：新建绑定=生效中（未存档、未申请删除）。 */
    public AmusementProjectEntitlementRecord(String id, String projectId, String entitlementType, Instant createdAt) {
        this(id, projectId, entitlementType, createdAt, null, null, null, "none", null, null, null);
    }

    /** 生效中=未存档且未被审批通过删除（前台可见、核销绑定校验采信）。 */
    public boolean active() {
        return archivedAt == null && !"approved".equals(deleteStatus);
    }
}
