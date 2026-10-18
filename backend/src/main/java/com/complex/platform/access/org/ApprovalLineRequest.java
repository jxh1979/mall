package com.complex.platform.access.org;

// 文件版本：V0.01

/** M087-A-03（PGM-1.7.1「每个部门或业务线须预先设定其平行审批的最低起点层级」）：平行审批线设置。 */
public record ApprovalLineRequest(
        String idempotencyKey,
        String operatorNote,
        String lineName,
        Integer minStartRank,
        String entryUserId,
        String status,
        String hrFinalApproverId
) {
    public ApprovalLineRequest(String idempotencyKey, String operatorNote, String lineName, Integer minStartRank,
            String entryUserId, String status) {
        this(idempotencyKey, operatorNote, lineName, minStartRank, entryUserId, status, null);
    }
}
