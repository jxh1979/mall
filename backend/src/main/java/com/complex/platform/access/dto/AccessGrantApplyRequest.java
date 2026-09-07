package com.complex.platform.access.dto;

import java.time.Instant;
import java.util.List;

/** 授权申请入参。M087（PGM-1.7.1「发起单据时只需指定该事项的最终审批人」）：finalApproverId 可指定主线最终审批人。 */
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
        String reason,
        String finalApproverId
) {
    /** 向后兼容构造：不指定最终审批人（按档位下限沿直接上级链生成）。 */
    public AccessGrantApplyRequest(
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
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, requestType, targetUserId, roleId, positionCode, staffLevelId,
                baseSalaryAmountCent, salaryCurrency, salaryDetail, permissionCodes, validFrom, expiresAt, singleUse, reason, null);
    }
}
