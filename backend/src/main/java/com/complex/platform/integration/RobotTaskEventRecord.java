package com.complex.platform.integration;

import java.time.Instant;
import java.util.Map;

/**
 * R5 T14 机器人被动留痕（写回调）。与 T17 dispatch 段对接：handoverId 预留关联交接。
 */
public record RobotTaskEventRecord(
        String id,
        String robotTaskRef,
        String eventType,
        String handoverId,
        Map<String, Object> payload,
        Instant reportedAt
) {
}
