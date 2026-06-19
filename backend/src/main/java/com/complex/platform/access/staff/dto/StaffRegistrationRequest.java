package com.complex.platform.access.staff.dto;

/**
 * 员工自助注册请求（私人手机 App 提交）。个人信息只传不可逆哈希（phone_hash / id_doc_hash），
 * 后端不再二次保存明文（PIPL）。project_id 必填（隔离）；merchant_id 可空（NULL=平台员工）。
 */
public record StaffRegistrationRequest(
        String idempotencyKey,
        String operatorNote,
        String projectId,
        String merchantId,
        String name,
        String phoneHash,
        String idDocHash,
        String staffLevelId
) {
}
