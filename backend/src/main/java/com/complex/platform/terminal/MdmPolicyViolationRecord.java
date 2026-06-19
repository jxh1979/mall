package com.complex.platform.terminal;

import java.time.Instant;

/**
 * R5 T9 MDM 自有侧：设备策略违规留痕（私装软件 / 访问非白名单 / 绕过企微 等）。
 *
 * <p>P0#9 增 {@code disposition}（处置类型）+ {@code blockedAt}（封禁联动时间），
 * 配合 handled 处置端点闭环（见 {@code MdmService#handleViolation}）。
 */
public record MdmPolicyViolationRecord(
        String id,
        String deviceId,
        String violationType,
        String detail,
        Instant occurredAt,
        boolean handled,
        String handledNote,
        String disposition,
        Instant blockedAt,
        Instant createdAt
) {
}
