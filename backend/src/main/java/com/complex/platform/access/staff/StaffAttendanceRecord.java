package com.complex.platform.access.staff;

import java.time.Instant;

/** E2(2026-07-02 拍板):打卡记录——服务器时间;严格无宽限判定;NFC/扫码标签原文与设备指纹留痕。 */
public record StaffAttendanceRecord(
        String id,
        String userId,
        String punchType,
        Instant punchedAt,
        String method,
        String tagRef,
        String spaceId,
        String deviceFingerprint,
        String shiftId,
        String judgement,
        Instant createdAt
) {
}
