package com.complex.platform.access.staff.dto;

/** 解绑 / 失效设备凭证。reason 记入 revoked_reason。 */
public record StaffDeviceRevokeRequest(
        String idempotencyKey,
        String operatorNote,
        String reason
) {
}
