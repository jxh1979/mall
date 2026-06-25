package com.complex.platform.facility.dto;

/** 楼宇设备状态流转请求（③ S1）：active|maintenance|retired。 */
public record FacilityStatusRequest(
        String status,
        String idempotencyKey
) {
}
