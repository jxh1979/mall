package com.complex.platform.contract;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 退租/到期结算：逐例顺序执行 + 争议挂起（in_progress/disputed/settled）。 */
public record ContractSettlementRecord(
        String id,
        String contractId,
        String triggerType,     // move_out / expiry / early_termination
        String status,          // in_progress / disputed / settled
        List<Map<String, Object>> settlementItems, // 逐例执行结果
        String disputeReason,
        String resolvedBy,
        Instant resolvedAt,
        String projectId,
        Instant createdAt,
        Instant updatedAt
) {
}
