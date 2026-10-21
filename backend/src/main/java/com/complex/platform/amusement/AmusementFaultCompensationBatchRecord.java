package com.complex.platform.amusement;

// 文件版本：V0.03

import java.time.Instant;

/**
 * CG-08b 补偿批次：对某次全园故障事件的一次批量发放。发起＝按故障发生时间快照受影响人份/次份（待确认）；
 * 另一岗位、另一人确认＝实际发放（另补一次完整票面权益；TK-09 裁决「没来的一律不延期」后不再有延期项）。
 * TK-18（2026-10-18 操作员裁决）：eventType=session_early_end＝场次提前结束（关联结束原件与场次，无全园故障）——批次由结束事务
 * 系统生成、无发起岗位；第一次岗位确认记为 initiated_*，仍待另一岗位、另一人确认后才发放。
 */
public record AmusementFaultCompensationBatchRecord(
        String id,
        String faultId,
        String projectId,
        String status,
        String reason,
        String bearerCompanyId,
        int validityDays,
        Instant windowFrom,
        Instant windowTo,
        Instant validUntil,
        String initiatedBy,
        String initiatedRole,
        Instant initiatedAt,
        String confirmedBy,
        String confirmedRole,
        Instant confirmedAt,
        int itemCount,
        int reissueCount,
        Instant createdAt,
        Instant updatedAt,
        String eventType,
        String endingId,
        String sessionId
) {
    public static final String EVENT_PARK_WIDE_FAULT = "park_wide_fault";
    public static final String EVENT_SESSION_EARLY_END = "session_early_end";
    /** TK-05-R3（BSM:804）：多次通票安全事件恢复本次 1 次完整补偿使用机会（夜班值班经理及以上批准即发放，关联安全事件）。 */
    public static final String EVENT_SAFETY_RESTORE = "safety_restore";

    /** 场次提前结束批次：第一次岗位确认记为发起方，状态仍为待确认。 */
    public AmusementFaultCompensationBatchRecord withFirstConfirmation(String actorId, String role, Instant at) {
        return new AmusementFaultCompensationBatchRecord(id, faultId, projectId, status, reason, bearerCompanyId,
                validityDays, windowFrom, windowTo, validUntil, actorId, role, at, confirmedBy, confirmedRole, confirmedAt,
                itemCount, reissueCount, createdAt, at, eventType, endingId, sessionId);
    }

    public AmusementFaultCompensationBatchRecord withIssued(String confirmedBy, String confirmedRole, Instant at,
            Instant validUntil, int reissueCount) {
        return new AmusementFaultCompensationBatchRecord(id, faultId, projectId, "issued", reason, bearerCompanyId,
                validityDays, windowFrom, windowTo, validUntil, initiatedBy, initiatedRole, initiatedAt,
                confirmedBy, confirmedRole, at, itemCount, reissueCount, createdAt, at, eventType, endingId, sessionId);
    }
}
