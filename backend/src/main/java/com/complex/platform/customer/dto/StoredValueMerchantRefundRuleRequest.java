package com.complex.platform.customer.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * B1d⑤ phase3 商户级卡退款规则配置（发卡商户自设）。
 * refundAllowed 空=默认允许；refundRatioBps 空=默认 10000（全额）；范围 [0,10000] 由服务层校验。
 */
public record StoredValueMerchantRefundRuleRequest(
        @NotBlank String merchantId,
        Boolean refundAllowed,
        Integer refundRatioBps
) {
}
