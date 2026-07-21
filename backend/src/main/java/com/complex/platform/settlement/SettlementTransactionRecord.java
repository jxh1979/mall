package com.complex.platform.settlement;

import java.time.Instant;

/**
 * 结算重构批1：settlement_transactions 一行的领域记录（对应 V222 表）。
 * 批1只写 transaction_type='verification_settled'（核销结算双写）；unverified_expired 由后续过期归项目 Job 产生。
 */
public record SettlementTransactionRecord(
        String id,
        String transactionNo,
        String transactionType,
        String entitlementId,
        String orderId,
        String ticketTypeId,
        String verificationId,
        String entitlementType,
        int verifiedCount,
        int totalTimes,
        int numPeople,
        long allocatedRevenueCent,
        long verifiedAmountCent,
        long unverifiedAmountCent,
        String beneficiaryType,
        String merchantId,
        String projectId,
        String settlementStatus,
        Instant entitlementValidTo,
        // 结算重构批3-4(PRD §6.4)：专属某商户「不分摊」，核销双写时从权益带上定格；非空=该笔核销全额归该商户不进池。
        String exclusiveMerchantId,
        // AUD-012 结算状态机重构：业务发生时刻（核销/冲正/到期），account 按此落月窗（取代首插 created_at）。
        Instant businessAt
) {
}
