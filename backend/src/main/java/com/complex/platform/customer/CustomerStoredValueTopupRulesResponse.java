package com.complex.platform.customer;

// 文件版本：V0.01

/**
 * ROOT-03-R30（ENUM 第 86 行「储值卡的消费扣款顺序、赠送金的性质与退卡口径，须在客人充值之前告知」）：客人充值前读到的规则。
 * cardScope＝general（综合体通用卡）/ merchant（商户级卡）；consumeOrder＝principal_first / bonus_first；
 * 商户卡另给退款是否允许与退回比例（基点，发卡商户自设）。
 */
public record CustomerStoredValueTopupRulesResponse(
        String cardScope,
        String projectId,
        String merchantId,
        int bonusBps,
        String consumeOrder,
        String consumeOrderText,
        String bonusRuleText,
        String refundRuleText,
        Boolean refundAllowed,
        Integer refundRatioBps
) {
}
