package com.complex.platform.compliance;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ComplianceExpiryItemRecord(
        String id,
        String subjectType,
        String subjectId,
        String merchantId,
        String itemType,
        String itemName,
        String documentNo,
        String issuingAuthority,
        LocalDate validFrom,
        LocalDate validTo,
        List<Integer> reminderDays,
        String expiryAction,
        String status,
        String ownerUserId,
        String sourceType,
        boolean humanConfirmed,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt,
        // 第十八批 E 段第 22 条：宽限申请状态（pending_review/approved/rejected，未申请为空）与申请时固化的宽限截止日。
        String graceStatus,
        LocalDate graceUntil
) {
}
