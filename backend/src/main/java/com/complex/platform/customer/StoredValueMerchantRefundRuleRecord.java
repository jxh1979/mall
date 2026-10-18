package com.complex.platform.customer;

import java.time.Instant;

/**
 * B1d⑤ phase3 商户级卡规则（发卡商户 + 平台配置）。
 * <ul>
 *   <li>refundAllowed=false → 该商户的商户卡拒退；refundRatioBps=可退回客户比例（基点 0..10000），余下为商户留存。
 *       （发卡商户自设，商户员工可改本商户。）</li>
 *   <li>cardIssuanceEnabled=发卡资格：平台后台开通后客人才能给该商户卡充值（默认关，仅平台可改）。</li>
 *   <li>fundOwnership=资金模式：merchant_prepaid=商户预收（商户自收）；platform_collect=平台代收、消费时逐笔结算给商户。
 *       仅平台可改，且只影响之后新开的卡（开卡时定格到卡）。</li>
 * </ul>
 * 仅对商户级卡（merchant_id 非空）生效；无配置=默认拒发卡、允许退、全额（10000）、商户预收。
 */
public record StoredValueMerchantRefundRuleRecord(
        String merchantId,
        boolean refundAllowed,
        int refundRatioBps,
        boolean cardIssuanceEnabled,
        String fundOwnership,
        Instant createdAt,
        Instant updatedAt,
        // CG-06a：商户级卡消费扣款顺序（BSM「支付方式」：由发卡商户自定、由平台运营代为录入后台）：principal_first / bonus_first。
        String consumeOrder
) {
    public static final String CONSUME_PRINCIPAL_FIRST = "principal_first";
    public static final String CONSUME_BONUS_FIRST = "bonus_first";

    /** 兼容构造（7 参）：扣款顺序取先本金后赠送金。 */
    public StoredValueMerchantRefundRuleRecord(String merchantId, boolean refundAllowed, int refundRatioBps,
            boolean cardIssuanceEnabled, String fundOwnership, Instant createdAt, Instant updatedAt) {
        this(merchantId, refundAllowed, refundRatioBps, cardIssuanceEnabled, fundOwnership, createdAt, updatedAt,
                CONSUME_PRINCIPAL_FIRST);
    }

    /** 缺省规则（无配置时）：不发卡、允许退、全额可退、商户预收、先本金后赠送金。 */
    public static StoredValueMerchantRefundRuleRecord defaultAllowed(String merchantId) {
        return new StoredValueMerchantRefundRuleRecord(merchantId, true, 10000,
                false, "merchant_prepaid", null, null, CONSUME_PRINCIPAL_FIRST);
    }

    /** 向后兼容构造（仅退款规则，5 参）：发卡资格默认关、资金模式默认商户预收。供既有调用方/映射零改动。 */
    public StoredValueMerchantRefundRuleRecord(
            String merchantId, boolean refundAllowed, int refundRatioBps, Instant createdAt, Instant updatedAt) {
        this(merchantId, refundAllowed, refundRatioBps, false, "merchant_prepaid", createdAt, updatedAt);
    }
}
