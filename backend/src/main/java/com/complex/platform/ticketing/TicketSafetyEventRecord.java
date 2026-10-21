package com.complex.platform.ticketing;

// 文件版本：V0.01

import java.time.Instant;

/**
 * TK-05 通票安全事件（BSM:804「通票安全例外」；V526 ticket_safety_events）：原票下一个人份（多次票为本次入场的次份）的一次安全事件，
 * 记录原因、当时情形、发起人、批准人及其依据；outcome=refund 免除该人份票款（refundId），outcome=restore 恢复本次 1 次完整补偿使用机会
 * （compensationBatchId）。
 */
public record TicketSafetyEventRecord(
        String id,
        String projectId,
        String orderId,
        String parentTicketId,
        int slotIndex,
        String entitlementId,
        String entryVerificationId,
        String sessionId,
        String reason,
        String situation,
        String responsibleEntityId,
        String outcome,
        String status,
        String requestedBy,
        Instant requestedAt,
        String approvedBy,
        Instant approvedAt,
        String approverBasis,
        String refundId,
        String compensationBatchId
) {
}
