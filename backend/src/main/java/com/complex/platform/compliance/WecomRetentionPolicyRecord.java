package com.complex.platform.compliance;

import java.time.Instant;

/** R5 T8 企微留存策略：scope + retention_days（默认 180）+ 到期动作 archive/delete/anonymize。 */
public record WecomRetentionPolicyRecord(
        String id,
        String scopeType,
        String scopeId,
        int retentionDays,
        String expiryAction,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
