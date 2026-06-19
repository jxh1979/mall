package com.complex.platform.inventory.dto;

/**
 * R5 T17 机器人段(dispatch)上报（被动留痕）：受 T14 integration.robot.sync 开关门控。
 * status：
 *   - delivered            → dispatch step done（机器人成功送达）。
 *   - delivered_no_receiver→ dispatch step gap（送达但无人工接收）→ 进复核。
 *   - 其它（failed 等）     → dispatch step diff（机器人与人工预期不符）→ 进复核。
 * reportDiff：上报点位/物料与人工预期不符时由调用方显式标记 → diff。
 */
public record TraceRobotReportRequest(
        String idempotencyKey,
        String robotTaskRef,
        String status,
        Boolean reportDiff
) {
}
