package com.complex.platform.amusement;

import java.time.Instant;

/**
 * CG-08b 补偿批次：对某次全园故障事件的一次批量发放。发起＝按故障发生时间快照受影响人份/次份（待确认）；
 * 另一岗位、另一人确认＝实际发放（延长核销权 / 另补一次完整票面权益）。
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
        int extendCount,
        int reissueCount,
        Instant createdAt,
        Instant updatedAt
) {
    public AmusementFaultCompensationBatchRecord withIssued(String confirmedBy, String confirmedRole, Instant at,
            Instant validUntil, int extendCount, int reissueCount) {
        return new AmusementFaultCompensationBatchRecord(id, faultId, projectId, "issued", reason, bearerCompanyId,
                validityDays, windowFrom, windowTo, validUntil, initiatedBy, initiatedRole, initiatedAt,
                confirmedBy, confirmedRole, at, itemCount, extendCount, reissueCount, createdAt, at);
    }
}
