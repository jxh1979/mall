// 文件版本：V0.01
package com.complex.platform.compliance;

import java.time.Instant;

/** 永久保存只读规则；retentionDays 为 null，expiryAction 为 permanent。 */
public record WecomRetentionPolicyRecord(
        String id,
        String scopeType,
        String scopeId,
        Integer retentionDays,
        String expiryAction,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
