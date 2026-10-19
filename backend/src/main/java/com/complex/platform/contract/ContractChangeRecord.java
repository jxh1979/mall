package com.complex.platform.contract;

// 文件版本：V0.01

import java.time.Instant;
import java.util.Map;

/**
 * 合同变更（6类，删转租；全走审批变更流）。newSnapshot=变更生效后的新版本快照（不改历史固化）。
 * reviewId（V494 contract_changes.review_id，HP-C01）=发起时按「变更」类模板开的统一审批复核单，终态回写 approvalStatus。
 */
public record ContractChangeRecord(
        String id,
        String contractId,
        String changeType,      // renewal/move_out/area_change/rent_change/business_scope_change/early_termination
        Map<String, Object> payload,
        String approvalStatus,  // pending / approved / rejected
        Map<String, Object> newSnapshot,
        String projectId,
        Instant createdAt,
        Instant updatedAt,
        String reviewId
) {
    /** 兼容构造（无复核单）：测试夹具沿用，reviewId 为空。 */
    public ContractChangeRecord(String id, String contractId, String changeType, Map<String, Object> payload,
            String approvalStatus, Map<String, Object> newSnapshot, String projectId, Instant createdAt, Instant updatedAt) {
        this(id, contractId, changeType, payload, approvalStatus, newSnapshot, projectId, createdAt, updatedAt, null);
    }
}
