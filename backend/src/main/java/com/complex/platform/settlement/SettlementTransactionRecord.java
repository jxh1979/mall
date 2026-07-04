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
        Instant entitlementValidTo
) {
}
