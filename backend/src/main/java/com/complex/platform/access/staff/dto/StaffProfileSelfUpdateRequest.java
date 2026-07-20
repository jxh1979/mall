package com.complex.platform.access.staff.dto;

/**
 * BYOD 二期 Wave1（2026-07-19 拍板·员工自助）：员工改本人个人信息（有限字段）。
 * 当前员工档案可自助编辑的有限字段仅显示姓名 name（手机/证件仅存不可逆哈希，不可回显亦不在本请求内改）。
 * 身份由 token 派生（StaffService#updateMyProfile 用 currentActor().id() 定位本人档案），请求体不含 userId 防越权。
 */
public record StaffProfileSelfUpdateRequest(
        String idempotencyKey,
        String name
) {
}
