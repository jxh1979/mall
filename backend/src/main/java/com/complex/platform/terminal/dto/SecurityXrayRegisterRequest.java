package com.complex.platform.terminal.dto;

// 文件版本：V0.01

/**
 * 第 63 条（PGM:623「建设备台账，记录编号、型号、点位、责任人和状态」）：后台登记安检 X 光设备。
 * 编号、型号、绑定点位（空间）、责任人（员工档案）四项必填。
 */
public record SecurityXrayRegisterRequest(
        String idempotencyKey,
        String deviceCode,
        String deviceModel,
        String boundSpaceId,
        String staffId
) {
}
