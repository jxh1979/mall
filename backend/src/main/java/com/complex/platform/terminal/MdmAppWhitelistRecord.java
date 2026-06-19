package com.complex.platform.terminal;

import java.time.Instant;

/** R5 T9 MDM 自有侧：预装/允许 app 白名单条目。 */
public record MdmAppWhitelistRecord(
        String id,
        String packageName,
        String appLabel,
        String scopeType,
        String scopeId,
        boolean enabled,
        String reason,
        Instant createdAt,
        Instant updatedAt
) {
}
