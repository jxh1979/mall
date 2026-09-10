package com.complex.platform.access.staff.dto;

// 文件版本：V0.02

/**
 * 员工注册资料。phone/id_doc 传原值，由服务端加密存放，日常返回脱敏值。
 * project_id 必填；merchant_id 必填（商户代提交）；公司员工本人注册走公开入口 /staff/self-registrations（U14-007）。
 */
public record StaffRegistrationRequest(
        String idempotencyKey,
        String operatorNote,
        String projectId,
        String merchantId,
        String name,
        String phone,
        String idDoc,
        String staffLevelId
) {
}
