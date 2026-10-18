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
        Instant updatedAt,
        /** BSM:1231：尾款逾期暂停新增账期订单（全款订单仍允许），全部逾期货款结清后解除。 */
        boolean creditSuspended
) {
    public DealerRecord(String id, String projectId, String dealerName, String unifiedSocialCreditCode, String legalRepresentative, String contactName,
            String contactPhone, String status, boolean companyProfileReviewed, boolean contractReviewed, boolean invoiceReviewed, boolean bankReviewed,
            boolean portalEnabled, String reviewNote, String reviewedBy, Instant reviewedAt, String deactivationReason, String deactivatedBy,
            Instant deactivatedAt, String createdBy, Instant createdAt, Instant updatedAt) {
        this(id, projectId, dealerName, unifiedSocialCreditCode, legalRepresentative, contactName, contactPhone, status, companyProfileReviewed,
                contractReviewed, invoiceReviewed, bankReviewed, portalEnabled, reviewNote, reviewedBy, reviewedAt, deactivationReason, deactivatedBy,
                deactivatedAt, createdBy, createdAt, updatedAt, false);
    }
}
