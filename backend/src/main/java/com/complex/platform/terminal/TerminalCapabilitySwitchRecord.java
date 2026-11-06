package com.complex.platform.terminal;

import java.time.Instant;

/**
 * capabilityName：第十八批 W 段第 194 条 194-R1（PGM:287「……枚举取值……必须全部为中文」）——能力中文名，
 * 只在开关列表里取权限中文名（permissions.permission_name）；非权限类能力与其他读写路径为空。
 */
public record TerminalCapabilitySwitchRecord(
        String id,
        String terminalType,
        String capabilityCode,
        String scopeType,
        String scopeId,
        boolean enabled,
        String reason,
        Instant createdAt,
        Instant updatedAt,
        String capabilityName
) {
    public TerminalCapabilitySwitchRecord(String id, String terminalType, String capabilityCode, String scopeType, String scopeId,
                                          boolean enabled, String reason, Instant createdAt, Instant updatedAt) {
        this(id, terminalType, capabilityCode, scopeType, scopeId, enabled, reason, createdAt, updatedAt, null);
    }
}
