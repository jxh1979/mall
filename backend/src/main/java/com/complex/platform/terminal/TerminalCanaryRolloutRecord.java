package com.complex.platform.terminal;

import java.time.Instant;

/**
 * 终端开关灰度试点（V342）。每个「端 × 能力」最多一条；{@code active=false} 时灰度层弃权。
 * {@code terminalType} 可为跨端通配 '*'。
 */
public record TerminalCanaryRolloutRecord(
        String id,
        String terminalType,
        String capabilityCode,
        boolean active,
        String reason,
        Instant createdAt,
        Instant updatedAt
) {
}
