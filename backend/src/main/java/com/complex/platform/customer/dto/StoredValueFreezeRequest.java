package com.complex.platform.customer.dto;

/** 储值账户冻结/解冻请求（后台风控）。 */
public record StoredValueFreezeRequest(
        String projectId,
        String merchantId,
        String reason
) {
}
