package com.complex.platform.access.staff.dto;

/**
 * 端侧完整性上报。integrity_status 非 trusted（root_detected/debug_detected/abnormal）→
 * 凭证自动 revoked + 发 staff_device.integrity_failed 事件 + 通知复核。
 */
public record StaffDeviceIntegrityReportRequest(
        String idempotencyKey,
        String operatorNote,
        String integrityStatus
) {
}
