package com.complex.platform.contract;

import java.time.Instant;
import java.util.Map;

/** 合同变更（6类，删转租；全走审批变更流）。newSnapshot=变更生效后的新版本快照（不改历史固化）。 */
public record ContractChangeRecord(
        String id,
        String contractId,
        String changeType,      // renewal/move_out/area_change/rent_change/business_scope_change/early_termination
        Map<String, Object> payload,
        String approvalStatus,  // pending / approved / rejected
        Map<String, Object> newSnapshot,
        String projectId,
        Instant createdAt,
        Instant updatedAt
) {
}
