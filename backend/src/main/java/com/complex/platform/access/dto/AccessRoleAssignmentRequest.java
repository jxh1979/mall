package com.complex.platform.access.dto;

/** 用户↔角色装配请求（④ S1）。roleId 必填。 */
public record AccessRoleAssignmentRequest(
        String roleId,
        String idempotencyKey
) {
}
