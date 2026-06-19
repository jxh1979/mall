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
        Instant updatedAt
) {
}
