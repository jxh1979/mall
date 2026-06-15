package com.complex.platform.governance.dto;

import java.util.List;

public record ApprovalRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String decision,
        String comment,
        String nextAssigneeId
) {
}
