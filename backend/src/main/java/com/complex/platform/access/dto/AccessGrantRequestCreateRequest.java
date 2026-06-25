package com.complex.platform.access.dto;

/** 高危授权申请创建请求（④ S2）。requestType=user_role 需 userId+roleId；=role_permission 需 roleId+permissionCode。 */
public record AccessGrantRequestCreateRequest(
        String requestType,
        String userId,
        String roleId,
        String permissionCode,
        String reason,
        String idempotencyKey
) {
}
