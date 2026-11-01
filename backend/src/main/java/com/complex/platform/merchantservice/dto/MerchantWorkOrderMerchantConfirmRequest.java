package com.complex.platform.merchantservice.dto;

// 文件版本：V0.01

/**
 * 第十八批 L 段第 102 条 102-R2：商户在商户专区「我的工单」确认已解决即关闭（BSM:1385「商户投诉须由商户确认投诉已解决」
 * 「满意度在回访/验收时由提单方评分」）。satisfactionScore 可空，填则 1～5。
 */
public record MerchantWorkOrderMerchantConfirmRequest(
        String idempotencyKey,
        Integer satisfactionScore
) {
}
