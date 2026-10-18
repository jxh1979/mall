package com.complex.platform.amusement;

import java.time.Instant;

/**
 * CG-08a 全园故障状态（ENUM「全园故障与补偿通票」）：按综合体项目一条记录。
 * status：pending_activation（已发起，待第二岗位确认生效）→ active（生效，停售纯游乐票与含游乐权益通票）
 * → pending_lift（已发起解除，待第二岗位确认）→ lifted（已解除，恢复销售）。
 * 岗位：amusement_head＝游乐公司负责人；engineering_safety＝工程或安全岗位。
 */
public record AmusementParkWideFaultRecord(
        String id,
        String projectId,
        String status,
        String reason,
        String initiatedBy,
        String initiatedRole,
        Instant initiatedAt,
        String activationConfirmedBy,
        String activationConfirmedRole,
        Instant activatedAt,
        String liftInitiatedBy,
        String liftInitiatedRole,
        Instant liftInitiatedAt,
        String liftReason,
        String liftConfirmedBy,
        String liftConfirmedRole,
        Instant liftedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public AmusementParkWideFaultRecord withActivation(String confirmedBy, String confirmedRole, Instant at) {
        return new AmusementParkWideFaultRecord(id, projectId, "active", reason, initiatedBy, initiatedRole, initiatedAt,
                confirmedBy, confirmedRole, at, liftInitiatedBy, liftInitiatedRole, liftInitiatedAt, liftReason,
                liftConfirmedBy, liftConfirmedRole, liftedAt, createdAt, at);
    }

    public AmusementParkWideFaultRecord withLiftInitiated(String by, String role, String liftReason, Instant at) {
        return new AmusementParkWideFaultRecord(id, projectId, "pending_lift", reason, initiatedBy, initiatedRole, initiatedAt,
                activationConfirmedBy, activationConfirmedRole, activatedAt, by, role, at, liftReason,
                liftConfirmedBy, liftConfirmedRole, liftedAt, createdAt, at);
    }

    public AmusementParkWideFaultRecord withLifted(String confirmedBy, String confirmedRole, Instant at) {
        return new AmusementParkWideFaultRecord(id, projectId, "lifted", reason, initiatedBy, initiatedRole, initiatedAt,
                activationConfirmedBy, activationConfirmedRole, activatedAt, liftInitiatedBy, liftInitiatedRole, liftInitiatedAt,
                liftReason, confirmedBy, confirmedRole, at, createdAt, at);
    }
}
