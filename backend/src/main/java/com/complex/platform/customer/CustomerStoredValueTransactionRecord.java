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
        Instant createdAt,
        // V231 强制口径：充值即支付，topup 流水在充值成功时追加，paidAt=交易完成时刻(=createdAt)；consume/refund/adjust 非充值支付为 null。
        Instant paidAt,
        // CG-06a：本笔变动中的本金 / 赠送金部分（带方向）；amountCent = principalCent + bonusCent。
        long principalCent,
        long bonusCent
) {
    /** 兼容构造（11 参，无两段）：本金 = 全额、赠送金 = 0——仅供旧替身，生产路径一律用全参构造。 */
    public CustomerStoredValueTransactionRecord(
            String id,
            String accountId,
            String customerId,
            String txnType,
            long amountCent,
            long balanceAfterCent,
            String paymentIntentId,
            String reason,
            String idempotencyKey,
            Instant createdAt,
            Instant paidAt
    ) {
        this(id, accountId, customerId, txnType, amountCent, balanceAfterCent, paymentIntentId, reason, idempotencyKey,
                createdAt, paidAt, amountCent, 0L);
    }

    /** V231 兼容构造器（10 参，无 paidAt）：paidAt 默认 null。 */
    public CustomerStoredValueTransactionRecord(
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
        this(id, accountId, customerId, txnType, amountCent, balanceAfterCent,
                paymentIntentId, reason, idempotencyKey, createdAt, null);
    }
}
