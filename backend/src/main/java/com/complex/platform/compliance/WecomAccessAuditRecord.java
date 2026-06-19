package com.complex.platform.compliance;

import java.time.Instant;

/** R5 T8 企微原文调阅审计：viewer/view_type(redacted|raw)/approval_ref/viewed_at。 */
public record WecomAccessAuditRecord(
        String id,
        String conversationId,
        String viewerUserId,
        String viewType,
        String approvalRef,
        Instant viewedAt
) {
}
