package com.complex.platform.terminal;

import java.time.Instant;

/** R5 T9 MDM 自有侧：设备策略违规留痕（私装软件 / 访问非白名单 / 绕过企微 等）。 */
public record MdmPolicyViolationRecord(
        String id,
        String deviceId,
        String violationType,
        String detail,
        Instant occurredAt,
        boolean handled,
        String handledNote,
        Instant createdAt
) {
}
