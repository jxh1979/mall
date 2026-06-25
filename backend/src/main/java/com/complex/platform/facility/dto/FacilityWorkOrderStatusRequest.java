package com.complex.platform.facility.dto;

/** 楼宇维保工单状态流转请求（③ S2）。status 必填且为合法迁移目标。 */
public record FacilityWorkOrderStatusRequest(
        String status,
        String idempotencyKey
) {
}
