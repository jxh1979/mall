package com.complex.platform.customer;

// 文件版本：V0.01

import java.time.Instant;

/**
 * ROOT-03-R30 客人端「我的余额」逐笔记录（BSM 第 868 行「每笔充值（时间、金额、本金与赠送金拆分、支付通路）、每笔扣款（商户、扣减的本金与赠送金
 * 各多少）、每笔退款与回冲（含回冲顺序结果）」）：在储值流水上补充值支付通路（通用卡取充值批次、商户卡取支付意图渠道）、扣款商户（商户卡取发卡商户，
 * 通用卡买单店票取该票所属酒吧；通票为空＝综合体）与回冲所指原消费流水。
 */
public record CustomerStoredValueTransactionView(
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
        Instant paidAt,
        long principalCent,
        long bonusCent,
        String paymentChannel,
        String merchantId,
        String merchantName,
        String originalTxnId
) {
}
