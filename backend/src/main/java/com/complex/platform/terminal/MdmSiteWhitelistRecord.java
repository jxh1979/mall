package com.complex.platform.terminal;

import java.time.Instant;

/** R5 T9 MDM 自有侧：允许访问网站域名白名单条目。 */
public record MdmSiteWhitelistRecord(
        String id,
        String domain,
        String description,
        String scopeType,
        String scopeId,
        boolean enabled,
        String reason,
        Instant createdAt,
        Instant updatedAt
) {
}
