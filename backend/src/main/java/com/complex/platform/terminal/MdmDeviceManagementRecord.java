package com.complex.platform.terminal;

import java.time.Instant;

/**
 * R5 T9 MDM 自有侧：terminal_devices 上的纳管视图（纳管/策略字段）。
 * 单独投影，避免膨胀既有 21 字段的 {@link TerminalDeviceRecord} 及其全部构造点。
 */
public record MdmDeviceManagementRecord(
        String deviceId,
        String terminalType,
        String status,
        String managementStatus,
        String policyStatus,
        Instant lastPolicySyncAt,
        Instant enrolledAt
) {
}
