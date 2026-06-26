package com.complex.platform.contract;

import java.time.Instant;

/**
 * 合同审批实例留痕（按所属链逐阶段并行走）：同 (subject,stage_seq) 多行 = 并行会签。
 * changeId 空 = 新签合同审批；非空 = 该合同变更走同一套链。
 */
public record ContractApprovalRecord(
        String id,
        String contractId,
        String changeId,
        int stageSeq,
        String approverPermissionCode,
        String approverUserId,
        String decision,        // pending / approved / rejected
        String comment,
        Instant decidedAt,
        Instant createdAt
) {
}
