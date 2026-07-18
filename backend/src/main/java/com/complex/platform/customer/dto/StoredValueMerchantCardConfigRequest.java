package com.complex.platform.customer.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 商户卡平台配置请求（仅平台可改）：
 * cardIssuanceEnabled=发卡资格（开通后客人才能给该商户卡充值）；
 * fundOwnership=资金模式（merchant_prepaid=商户预收 / platform_collect=平台代收），只影响之后新开的卡。
 */
public record StoredValueMerchantCardConfigRequest(
        @NotBlank String merchantId,
        Boolean cardIssuanceEnabled,
        String fundOwnership
) {
}
