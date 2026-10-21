package com.complex.platform.amusement;

// 文件版本：V0.03

import java.time.Instant;
import java.util.List;

/**
 * CG-08b 补偿记录：原票下一个受影响人份（多次票为一个次份）。kind：full_reissue（故障发生时已入场核销且本场次履约受影响：
 * 另补一次完整票面权益，含酒水、礼品）。TK-09（2026-10-21 操作员裁决补充第 3 条「没来的一律不延期」）：不再生成未核延期（extend）项。
 * 关联原票、人份/次份、原场次、故障事件与发放原因；同一故障下同一人份只补偿一次；补偿机会使用一次后即用尽（fulfilledVerificationId）。
 * TK-18：endingId＝场次提前结束的结束原件（此时 faultId 为空；已入场人份一律 full_reissue）。
 */
public record AmusementFaultCompensationItemRecord(
        String id,
        String batchId,
        String faultId,
        String projectId,
        String entitlementId,
        String orderId,
        String ticketTypeId,
        String parentTicketId,
        Integer slotIndex,
        String customerId,
        String kind,
        int shareCount,
        String originalSessionId,
        String originalEntryVerificationId,
        Instant originalValidTo,
        String status,
        Instant newValidTo,
        List<String> compensationEntitlementIds,
        String fulfilledVerificationId,
        Instant fulfilledAt,
        Long fulfilledSettlementPriceCent,
        Instant createdAt,
        Instant updatedAt,
        String endingId
) {
    public static final String KIND_FULL_REISSUE = "full_reissue";
}
