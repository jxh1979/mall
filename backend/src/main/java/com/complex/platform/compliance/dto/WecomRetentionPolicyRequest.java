package com.complex.platform.compliance.dto;

/** R5 T8 企微留存策略配置：scope + retention_days + 到期动作 archive/delete/anonymize。 */
public record WecomRetentionPolicyRequest(
        String idempotencyKey,
        String scopeType,
        String scopeId,
        Integer retentionDays,
        String expiryAction,
        Boolean enabled
) {
}
