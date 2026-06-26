package com.complex.platform.facility.dto;

/** 工单处理/验收/回访/挂起动作请求。 */
public record FacilityWorkOrderActionRequest(
        String resolution,
        Integer satisfaction,
        String note,
        String idempotencyKey) {}
