package com.complex.platform.customer;

/**
 * AUD-058 储值充值支付意图绑定的卡范围（V284 stored_value_topup_intents）：
 * topup 创建支付意图时落库，settleTopup 只认此范围入账，杜绝入账时请求体指名跨卡/跨商户/跨项目搬运资金。
 */
public record StoredValueTopupIntentScope(
        String paymentIntentId,
        String customerId,
        String projectId,
        String merchantId
) {
}
