package com.complex.platform.governance.dto;

import java.time.LocalDate;

/** 合同续约请求（② 履约闭环 S3）：基于 effective/expired 合同生成新 draft（复用原条款 + 新租期），链 renewed_from。 */
public record ContractRenewRequest(
        LocalDate effectiveDate,
        LocalDate expiryDate,
        String idempotencyKey
) {
}
