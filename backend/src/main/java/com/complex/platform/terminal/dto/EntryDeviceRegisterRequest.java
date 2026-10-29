package com.complex.platform.terminal.dto;

// 文件版本：V0.02

/**
 * 第 63 条（PGM:623「建设备台账，记录编号、型号、点位、责任人和状态」）、第 82 条 82c-R1（PGM:618「综合体入场三道环节设备
 * （安检环节的 X 光设备、年龄核验环节的人脸扫描设备、检票环节的检票闸机）」）：后台登记入场三道环节设备。
 * 类型（security_xray / face_scanner / ticket_gate）、编号、型号、绑定点位（空间）、责任人（员工档案）五项必填。
 * 第 89 条 89-R3（PGM:618「设备包括……扫码设备……和其他现场设备」）：类型另收 scanner（扫码设备）、other_site_device（其他现场设备）。
 */
public record EntryDeviceRegisterRequest(
        String idempotencyKey,
        String terminalType,
        String deviceCode,
        String deviceModel,
        String boundSpaceId,
        String staffId
) {
}
