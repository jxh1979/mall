package com.complex.platform.access.staff;

import java.time.Instant;

/**
 * 员工档案（R5 T1）。注册 → 审批状态机。
 * 隔离键：project_id（NOT NULL）、merchant_id（NULL=平台员工）。
 * 个人信息只存不可逆哈希（PIPL，compliance §4）：phone_hash / id_doc_hash。
 * registration_status: submitted/approved/rejected/suspended；status: active/inactive。
 */
public record StaffProfileRecord(
        String id,
        String projectId,
        String merchantId,
        String userId,
        String name,
        String phoneHash,
        String idDocHash,
        String staffLevelId,
        String registrationStatus,
        String approvedBy,
        Instant approvedAt,
        String rejectReason,
        String status,
        Instant createdAt,
        Instant updatedAt,
        // V264 数据存档与审批后删除范式（照 V259 商户先例）。存档=软标记可恢复；删除=硬删走审批。
        Instant archivedAt,
        String archivedBy,
        String deleteStatus,
        String deleteRequestedBy,
        Instant deleteRequestedAt,
        String deleteReason
) {

    /** 向后兼容构造器（V264 存档/删除前签名）：既有调用方零改动，默认未存档 + delete_status=none。 */
    public StaffProfileRecord(
            String id,
            String projectId,
            String merchantId,
            String userId,
            String name,
            String phoneHash,
            String idDocHash,
            String staffLevelId,
            String registrationStatus,
            String approvedBy,
            Instant approvedAt,
            String rejectReason,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, projectId, merchantId, userId, name, phoneHash, idDocHash, staffLevelId, registrationStatus,
                approvedBy, approvedAt, rejectReason, status, createdAt, updatedAt,
                null, null, "none", null, null, null);
    }

    // --- V264 存档/删除范式：不可变 record 的字段更新辅助。 ---
    public StaffProfileRecord withArchive(Instant archivedAt, String archivedBy, Instant updatedAt) {
        return new StaffProfileRecord(id, projectId, merchantId, userId, name, phoneHash, idDocHash, staffLevelId,
                registrationStatus, approvedBy, approvedAt, rejectReason, status, createdAt, updatedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
    }

    public StaffProfileRecord withDelete(String deleteStatus, String deleteRequestedBy,
            Instant deleteRequestedAt, String deleteReason, Instant updatedAt) {
        return new StaffProfileRecord(id, projectId, merchantId, userId, name, phoneHash, idDocHash, staffLevelId,
                registrationStatus, approvedBy, approvedAt, rejectReason, status, createdAt, updatedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
    }
}
