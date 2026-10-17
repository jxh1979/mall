package com.complex.platform.dealer;

import java.time.Instant;

/** 经销商档案。status：pending_submission 待提交 / under_review 审核中 / approved 已通过 / rejected 已驳回 / deactivated 已停用（ENUM:170）。 */
public record DealerRecord(
        String id,
        String projectId,
        String dealerName,
        String unifiedSocialCreditCode,
        String legalRepresentative,
        String contactName,
        String contactPhone,
        String status,
        boolean companyProfileReviewed,
        boolean contractReviewed,
        boolean invoiceReviewed,
        boolean bankReviewed,
        boolean portalEnabled,
        String reviewNote,
        String reviewedBy,
        Instant reviewedAt,
        String deactivationReason,
        String deactivatedBy,
        Instant deactivatedAt,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
