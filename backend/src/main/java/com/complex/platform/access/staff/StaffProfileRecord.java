package com.complex.platform.access.staff;

// 文件版本：V0.05

import java.time.Instant;

/**
 * 员工档案（R5 T1）。注册 → 审批状态机。
 * 隔离键：project_id（NOT NULL）、merchant_id（NULL=平台员工）、company_id（写入必填；兼容构造取 project_id——main 项目范围即所属组织/公司，U05-010）。
 * 姓名、手机号与证件原值由仓储统一加密；日常接口返回脱敏副本。
 * registration_status: submitted/approved/rejected/suspended；status: active/inactive。
 */
public record StaffProfileRecord(
        String id,
        String projectId,
        String merchantId,
        String userId,
        String name,
        String phone,
        String idDoc,
        String staffLevelId,
        String registrationStatus,
        String approvedBy,
        Instant approvedAt,
        String rejectReason,
        String status,
        Instant createdAt,
        Instant updatedAt,
        // V264 数据存档与审批后删除范式（照 V259 商户先例）。存档=软标记可恢复；删除=审批后软删除。
        Instant archivedAt,
        String archivedBy,
        String deleteStatus,
        String deleteRequestedBy,
        Instant deleteRequestedAt,
        String deleteReason,
        // V349 销售岗标识：卡座提成按员工岗位判档的唯一依据（TRUE=销售人员费率，FALSE=非销售人员费率）。
        // 出厂 FALSE——资金口径不就高不就低；由人事在「员工档案」页显式勾选，不按单次是否指派销售人员临时判。
        boolean salesPosition,
        String employmentStatus,
        String companyId,
        String registrationSource
) {

    /** 向后兼容构造器（V264 存档/删除前签名）：既有调用方零改动，默认未存档 + delete_status=none + 非销售岗。 */
    public StaffProfileRecord(
            String id,
            String projectId,
            String merchantId,
            String userId,
            String name,
            String phone,
            String idDoc,
            String staffLevelId,
            String registrationStatus,
            String approvedBy,
            Instant approvedAt,
            String rejectReason,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, projectId, merchantId, userId, name, phone, idDoc, staffLevelId, registrationStatus,
                approvedBy, approvedAt, rejectReason, status, createdAt, updatedAt,
                null, null, "none", null, null, null, false, "pending_entry", projectId, null);
    }

    public StaffProfileRecord(String id, String projectId, String merchantId, String userId, String name,
            String phone, String idDoc, String staffLevelId, String registrationStatus, String approvedBy,
            Instant approvedAt, String rejectReason, String status, Instant createdAt, Instant updatedAt,
            Instant archivedAt, String archivedBy, String deleteStatus, String deleteRequestedBy,
            Instant deleteRequestedAt, String deleteReason, boolean salesPosition) {
        this(id, projectId, merchantId, userId, name, phone, idDoc, staffLevelId, registrationStatus,
                approvedBy, approvedAt, rejectReason, status, createdAt, updatedAt, archivedAt, archivedBy,
                deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, salesPosition, "pending_entry", projectId, null);
    }

    public StaffProfileRecord(String id, String projectId, String merchantId, String userId, String name,
            String phone, String idDoc, String staffLevelId, String registrationStatus, String approvedBy,
            Instant approvedAt, String rejectReason, String status, Instant createdAt, Instant updatedAt,
            Instant archivedAt, String archivedBy, String deleteStatus, String deleteRequestedBy,
            Instant deleteRequestedAt, String deleteReason, boolean salesPosition, String employmentStatus) {
        this(id,projectId,merchantId,userId,name,phone,idDoc,staffLevelId,registrationStatus,approvedBy,approvedAt,
                rejectReason,status,createdAt,updatedAt,archivedAt,archivedBy,deleteStatus,deleteRequestedBy,
                deleteRequestedAt,deleteReason,salesPosition,employmentStatus,null,null);
    }

    public StaffProfileRecord withCompanyRoute(String companyId, String registrationSource) {
        return new StaffProfileRecord(id,projectId,merchantId,userId,name,phone,idDoc,staffLevelId,registrationStatus,
                approvedBy,approvedAt,rejectReason,status,createdAt,updatedAt,archivedAt,archivedBy,deleteStatus,
                deleteRequestedBy,deleteRequestedAt,deleteReason,salesPosition,employmentStatus,companyId,registrationSource);
    }

    public StaffProfileRecord withEmploymentStatus(String employmentStatus, Instant updatedAt) {
        return new StaffProfileRecord(id, projectId, merchantId, userId, name, phone, idDoc, staffLevelId,
                registrationStatus, approvedBy, approvedAt, rejectReason, status, createdAt, updatedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                salesPosition, employmentStatus, companyId, registrationSource);
    }

    public StaffProfileRecord maskedPersonalInfo() {
        return new StaffProfileRecord(id, projectId, merchantId, userId, name,
                com.complex.platform.common.PhoneMask.mask(phone), idDoc == null ? null : "******",
                staffLevelId, registrationStatus, approvedBy, approvedAt, rejectReason, status,
                createdAt, updatedAt, archivedAt, archivedBy, deleteStatus, deleteRequestedBy,
                deleteRequestedAt, deleteReason, salesPosition, employmentStatus, companyId, registrationSource);
    }

    public StaffProfileRecord withRegistrationReview(String userId, String registrationStatus,
            String approvedBy, Instant reviewedAt, String rejectReason) {
        return new StaffProfileRecord(id, projectId, merchantId, userId, name, phone, idDoc, staffLevelId,
                registrationStatus, approvedBy, reviewedAt, rejectReason, status, createdAt, reviewedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                salesPosition, employmentStatus, companyId, registrationSource);
    }

    // --- V264 存档/删除范式：不可变 record 的字段更新辅助。 ---
    public StaffProfileRecord withArchive(Instant archivedAt, String archivedBy, Instant updatedAt) {
        return new StaffProfileRecord(id, projectId, merchantId, userId, name, phone, idDoc, staffLevelId,
                registrationStatus, approvedBy, approvedAt, rejectReason, status, createdAt, updatedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                salesPosition, employmentStatus, companyId, registrationSource);
    }

    public StaffProfileRecord withDelete(String deleteStatus, String deleteRequestedBy,
            Instant deleteRequestedAt, String deleteReason, Instant updatedAt) {
        return new StaffProfileRecord(id, projectId, merchantId, userId, name, phone, idDoc, staffLevelId,
                registrationStatus, approvedBy, approvedAt, rejectReason, status, createdAt, updatedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                salesPosition, employmentStatus, companyId, registrationSource);
    }

    /** V349：设/撤销销售岗（人事在员工档案页维护，落审计）。 */
    public StaffProfileRecord withSalesPosition(boolean salesPosition, Instant updatedAt) {
        return new StaffProfileRecord(id, projectId, merchantId, userId, name, phone, idDoc, staffLevelId,
                registrationStatus, approvedBy, approvedAt, rejectReason, status, createdAt, updatedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                salesPosition, employmentStatus, companyId, registrationSource);
    }
}
