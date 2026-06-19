package com.complex.platform.customer;

import java.time.Instant;

/**
 * 储值余额变动流水（gap-analysis §7A P1）。
 * txnType=topup|consume|refund|adjust；amountCent 带方向（充值/退款为正入账，消费为负）；
 * balanceAfterCent=变动后余额；paymentIntentId=充值关联支付意图（预付资金合规）；
 * idempotencyKey 唯一防重复入账。
 */
public record CustomerStoredValueTransactionRecord(
        String id,
        String accountId,
        String customerId,
        String txnType,
        long amountCent,
        long balanceAfterCent,
        String paymentIntentId,
        String reason,
        String idempotencyKey,
        Instant createdAt
) {
}
