package com.complex.platform.customer.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 商户卡发卡设置请求（第十八批 CL 段 423-R1：只由发卡商户本商户账号在商户后台自行设置）：
 * cardIssuanceEnabled=发卡资格（开通后客人才能给该商户卡充值）；
 * fundOwnership=资金模式（merchant_prepaid=商户预收 / platform_collect=平台代收），只影响之后新开的卡。
 */
public record StoredValueMerchantCardConfigRequest(
        @NotBlank String merchantId,
        Boolean cardIssuanceEnabled,
        String fundOwnership,
        // CG-06a：商户级卡消费扣款顺序（principal_first / bonus_first），由发卡商户在商户后台自行设置；为空保留现值。
        String consumeOrder
) {
}
