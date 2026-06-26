package com.complex.platform.access;

import java.time.Instant;
import java.util.List;

public record AccessGrantRequestRecord(
        String id,
        String requestType,
        String targetUserId,
        String targetRoleId,
        String templateId,
        String positionCode,
        String staffLevelId,
        Long baseSalaryAmountCent,
        String salaryCurrency,
        String salaryDetail,
        List<String> permissionCodes,
        Instant validFrom,
        Instant expiresAt,
        boolean singleUse,
        String status,
        String reviewId,
        String reason,
        String requestedBy,
        String decidedBy,
        Instant decidedAt,
        String projectId,
        String merchantId,
        Instant createdAt,
        Instant updatedAt
) {
    public AccessGrantRequestRecord {
        permissionCodes = permissionCodes == null ? List.of() : List.copyOf(permissionCodes);
    }
}
