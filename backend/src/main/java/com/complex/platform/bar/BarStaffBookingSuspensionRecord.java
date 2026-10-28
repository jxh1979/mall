package com.complex.platform.bar;

// 文件版本：V0.01

import java.time.Instant;

/**
 * 第十八批 C 段第 15 条（BSM 第 1468 行「经手员工的爽约率超过后台配置的上限时……自动停用其代订权限，由店长复核后恢复」）：
 * 员工代订权限停用记录。merchantId＝酒吧（酒吧即商户，V267）；statMonth＝触发时统计的营业月（第十八批裁决 3 按自然月）。
 */
public record BarStaffBookingSuspensionRecord(
        String id,
        String merchantId,
        String staffUserId,
        String status,
        String statMonth,
        int noShowCount,
        int bookingCount,
        int limitPct,
        String triggerReservationId,
        Instant suspendedAt,
        String restoredBy,
        Instant restoredAt
) {

    /** 店长复核恢复（请求体只有幂等键：HTML 只写「由店长复核后恢复」，操作人与时间由记录与审计留痕）。 */
    public record RestoreRequest(String idempotencyKey) {
    }

    BarStaffBookingSuspensionRecord restored(String by, Instant at) {
        return new BarStaffBookingSuspensionRecord(id, merchantId, staffUserId, "restored", statMonth, noShowCount, bookingCount,
                limitPct, triggerReservationId, suspendedAt, by, at);
    }
}
