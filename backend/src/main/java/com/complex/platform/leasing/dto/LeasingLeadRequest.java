package com.complex.platform.leasing.dto;

/** 招商线索创建请求（② 履约闭环 S4）。 */
public record LeasingLeadRequest(
        String contactName,
        String intendedBusinessType,
        String intendedSpaceId,
        String source,
        String idempotencyKey
) {
}
