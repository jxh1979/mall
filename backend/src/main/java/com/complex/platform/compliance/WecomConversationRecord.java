package com.complex.platform.compliance;

import java.time.Instant;
import java.time.LocalDate;

/** R5 T8 企微会话归属：员工/客户/外部会话 id/项目 + 归档状态 + 留存到期日。 */
public record WecomConversationRecord(
        String id,
        String externalConversationId,
        String projectId,
        String ownerStaffId,
        String customerRef,
        String archiveStatus,
        LocalDate retentionUntil,
        Instant createdAt,
        Instant updatedAt
) {
}
