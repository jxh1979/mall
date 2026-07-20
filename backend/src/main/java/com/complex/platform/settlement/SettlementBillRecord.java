package com.complex.platform.settlement;

import java.time.LocalDate;

/** 结算重构批2-3：月度结算单头（settlement_bills 一行）。金额均整数分。 */
public record SettlementBillRecord(
        String id,
        String projectId,
        LocalDate periodStart,
        LocalDate periodEnd,
        long ticketRevenueCent,
        long channelFeeCent,
        long giveawayCostCent,
        long availablePoolCent,
        int retentionBps,
        long retentionCent,
        long spvPoolCent,
        long distributedCent,
        long compServiceFeeCent,
        String distributionMethod,
        String specialApprovalRef,
        String status,
        // 六业态收入归属(terminology-canonical §三):通票收入归属的 L3 管理公司/业态主体(娱乐运营平台)。
        // 由 revenue_stream_attribution 按 (项目, all_pass_ticket) 解析;未配置=null(零金额影响,仅归属留白)。
        String attributedEntityId
) {
}
