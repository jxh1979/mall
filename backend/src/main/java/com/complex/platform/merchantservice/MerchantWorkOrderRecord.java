package com.complex.platform.merchantservice;

import java.time.Instant;
import java.util.List;

public record MerchantWorkOrderRecord(
        String id,
        String merchantId,
        String spaceId,
        String contractId,
        String workOrderType,
        String priority,
        String status,
        String title,
        String description,
        String reportedByUserId,
        String reportedChannel,
        String ownerDepartment,
        String ownerUserId,
        Instant dueAt,
        Instant closedAt,
        String resolution,
        Integer satisfactionScore,
        List<String> evidenceAttachmentIds,
        boolean slaBreached,
        Instant escalatedAt,
        Instant createdAt,
        Instant updatedAt,
        // V264 数据存档与审批后删除范式（照 V259 商户先例）。存档=软标记可恢复；删除=硬删走审批。
        Instant archivedAt,
        String archivedBy,
        String deleteStatus,
        String deleteRequestedBy,
        Instant deleteRequestedAt,
        String deleteReason,
        // 第十八批 BY 段第 387 条 387-R1（第十八批裁决 81「每个分类都显示存档理由」）：含已存档列表带出存档理由（V271 archive_reason 列）。
        String archiveReason
) {
    /** 387-R1：存档理由之前的签名，既有构造点零改动，archiveReason 取空。 */
    public MerchantWorkOrderRecord(String id, String merchantId, String spaceId, String contractId, String workOrderType, String priority, String status, String title, String description, String reportedByUserId, String reportedChannel, String ownerDepartment, String ownerUserId, Instant dueAt, Instant closedAt, String resolution, Integer satisfactionScore, List<String> evidenceAttachmentIds, boolean slaBreached, Instant escalatedAt, Instant createdAt, Instant updatedAt, Instant archivedAt, String archivedBy, String deleteStatus, String deleteRequestedBy, Instant deleteRequestedAt, String deleteReason) {
        this(id, merchantId, spaceId, contractId, workOrderType, priority, status, title, description, reportedByUserId, reportedChannel, ownerDepartment, ownerUserId, dueAt, closedAt, resolution, satisfactionScore, evidenceAttachmentIds, slaBreached, escalatedAt, createdAt, updatedAt, archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, null);
    }

    public MerchantWorkOrderRecord {
        evidenceAttachmentIds = evidenceAttachmentIds == null ? List.of() : List.copyOf(evidenceAttachmentIds);
    }

    /** 向后兼容构造器（V264 存档/删除前签名）：既有调用方零改动，默认未存档 + delete_status=none。 */
    public MerchantWorkOrderRecord(
            String id,
            String merchantId,
            String spaceId,
            String contractId,
            String workOrderType,
            String priority,
            String status,
            String title,
            String description,
            String reportedByUserId,
            String reportedChannel,
            String ownerDepartment,
            String ownerUserId,
            Instant dueAt,
            Instant closedAt,
            String resolution,
            Integer satisfactionScore,
            List<String> evidenceAttachmentIds,
            boolean slaBreached,
            Instant escalatedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, merchantId, spaceId, contractId, workOrderType, priority, status, title, description,
                reportedByUserId, reportedChannel, ownerDepartment, ownerUserId, dueAt, closedAt, resolution,
                satisfactionScore, evidenceAttachmentIds, slaBreached, escalatedAt, createdAt, updatedAt,
                null, null, "none", null, null, null);
    }

    public MerchantWorkOrderRecord assign(String ownerDepartment, String ownerUserId, Instant now) {
        return new MerchantWorkOrderRecord(
                id, merchantId, spaceId, contractId, workOrderType, priority, "assigned", title, description,
                reportedByUserId, reportedChannel, ownerDepartment, ownerUserId, dueAt, closedAt, resolution,
                satisfactionScore, evidenceAttachmentIds, slaBreached, escalatedAt, createdAt, now,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, archiveReason);
    }

    public MerchantWorkOrderRecord transitionTo(String targetStatus, Instant now) {
        return new MerchantWorkOrderRecord(
                id, merchantId, spaceId, contractId, workOrderType, priority, targetStatus, title, description,
                reportedByUserId, reportedChannel, ownerDepartment, ownerUserId, dueAt, closedAt, resolution,
                satisfactionScore, evidenceAttachmentIds, slaBreached, escalatedAt, createdAt, now,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, archiveReason);
    }

    public MerchantWorkOrderRecord close(String resolution, Integer satisfactionScore, Instant now) {
        return new MerchantWorkOrderRecord(
                id, merchantId, spaceId, contractId, workOrderType, priority, "closed", title, description,
                reportedByUserId, reportedChannel, ownerDepartment, ownerUserId, dueAt, now, resolution,
                satisfactionScore, evidenceAttachmentIds, slaBreached, escalatedAt, createdAt, now,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, archiveReason);
    }

    /**
     * SLA 超时升级：仅打标 sla_breached + 记录 escalated_at，不改变 status，
     * 不破坏 ALLOWED_ACTION_TRANSITIONS 既有状态转移规则（升级是横切标记而非状态迁移）。
     */
    public MerchantWorkOrderRecord markSlaBreached(Instant now) {
        return new MerchantWorkOrderRecord(
                id, merchantId, spaceId, contractId, workOrderType, priority, status, title, description,
                reportedByUserId, reportedChannel, ownerDepartment, ownerUserId, dueAt, closedAt, resolution,
                satisfactionScore, evidenceAttachmentIds, true, now, createdAt, now,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, archiveReason);
    }

    // --- V264 存档/删除范式：不可变 record 的字段更新辅助。 ---
    public MerchantWorkOrderRecord withArchive(Instant archivedAt, String archivedBy, Instant updatedAt) {
        return new MerchantWorkOrderRecord(
                id, merchantId, spaceId, contractId, workOrderType, priority, status, title, description,
                reportedByUserId, reportedChannel, ownerDepartment, ownerUserId, dueAt, closedAt, resolution,
                satisfactionScore, evidenceAttachmentIds, slaBreached, escalatedAt, createdAt, updatedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, archivedAt == null ? null : archiveReason);
    }

    public MerchantWorkOrderRecord withDelete(String deleteStatus, String deleteRequestedBy,
            Instant deleteRequestedAt, String deleteReason, Instant updatedAt) {
        return new MerchantWorkOrderRecord(
                id, merchantId, spaceId, contractId, workOrderType, priority, status, title, description,
                reportedByUserId, reportedChannel, ownerDepartment, ownerUserId, dueAt, closedAt, resolution,
                satisfactionScore, evidenceAttachmentIds, slaBreached, escalatedAt, createdAt, updatedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, archiveReason);
    }
}
