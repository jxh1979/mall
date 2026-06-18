package com.complex.platform.evidence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

/** 服务责任链记录（留痕底层，REQ-37）。含财务责任段 refunded_by/waived_by/approved_by。merchant_id 由 token 注入。 */
public record TraceabilityResponsibilityRecord(
        String id,
        String orderId,
        String tableId,
        String customerId,
        String exceptionId,
        String primaryServiceStaffId,
        List<String> assistStaffIds,
        String salesStaffId,
        String firstHandlerStaffId,
        String refundedBy,
        String waivedBy,
        String approvedBy,
        String assignedBy,
        String shiftAssignmentId,
        String assignedEventId,
        @JsonIgnore String merchantId,
        Instant serviceStartAt,
        Instant serviceEndAt,
        String status
) {
}
