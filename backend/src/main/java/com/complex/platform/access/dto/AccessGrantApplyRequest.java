package com.complex.platform.access.dto;

import java.time.Instant;
import java.util.List;

public record AccessGrantApplyRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String requestType,
        String targetUserId,
        String roleId,
        String positionCode,
        String staffLevelId,
        Long baseSalaryAmountCent,
        String salaryCurrency,
        String salaryDetail,
        List<String> permissionCodes,
        Instant validFrom,
        Instant expiresAt,
        Boolean singleUse,
        String reason
) {
}
