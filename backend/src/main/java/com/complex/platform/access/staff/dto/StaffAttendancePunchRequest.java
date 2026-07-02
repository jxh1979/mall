package com.complex.platform.access.staff.dto;

/** E2 打卡请求(时间以服务器为准,客户端不传)。 */
public record StaffAttendancePunchRequest(
        String idempotencyKey,
        String punchType,
        String method,
        String tagRef,
        String spaceId,
        String deviceFingerprint
) {
}
