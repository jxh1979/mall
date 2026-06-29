package com.complex.platform.customer;

import java.time.Instant;

/**
 * B1d⑤ phase3 商户级卡退款规则（发卡商户自设）。
 * refundAllowed=false → 该商户的商户卡拒退；refundRatioBps=可退回客户比例（基点 0..10000），余下为商户留存。
 * 仅对商户级卡（merchant_id 非空）生效；无配置=默认允许、全额（10000）。
 */
public record StoredValueMerchantRefundRuleRecord(
        String merchantId,
        boolean refundAllowed,
        int refundRatioBps,
        Instant createdAt,
        Instant updatedAt
) {
    /** 缺省规则（无配置时）：允许、全额可退。 */
    public static StoredValueMerchantRefundRuleRecord defaultAllowed(String merchantId) {
        return new StoredValueMerchantRefundRuleRecord(merchantId, true, 10000, null, null);
    }
}
