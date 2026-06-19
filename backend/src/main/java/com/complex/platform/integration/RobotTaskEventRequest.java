package com.complex.platform.integration;

import java.util.Map;

/**
 * R5 T14 机器人上报请求。被动留痕，受 integration.robot.sync 开关门控。
 */
public record RobotTaskEventRequest(
        String robotTaskRef,
        String eventType,
        String handoverId,
        Map<String, Object> payload,
        String idempotencyKey
) {
}
