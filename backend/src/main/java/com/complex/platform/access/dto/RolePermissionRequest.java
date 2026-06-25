package com.complex.platform.access.dto;

/** 角色权限增删请求（④ S1）。permissionCode 必填。 */
public record RolePermissionRequest(
        String permissionCode,
        String idempotencyKey
) {
}
