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
        String distributionMethod,
        String specialApprovalRef,
        String status
) {
}
