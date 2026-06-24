package com.complex.platform.leasing.dto;

import java.time.LocalDate;
import java.util.Map;

/** 租赁报价创建请求（② 履约闭环 S4）：针对 leadId 的报价（空间/拟商户/租期/条款/有效期）。 */
public record LeasingOfferRequest(
        String leadId,
        String spaceId,
        String merchantId,
        LocalDate effectiveDate,
        LocalDate expiryDate,
        Map<String, Object> rentTerms,
        LocalDate validUntil,
        String idempotencyKey
) {
}
