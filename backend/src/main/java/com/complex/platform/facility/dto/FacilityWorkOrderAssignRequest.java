package com.complex.platform.facility.dto;

/** 工单派单请求。 */
public record FacilityWorkOrderAssignRequest(
        String assigneeId,
        String idempotencyKey) {}
