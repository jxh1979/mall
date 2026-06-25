package com.complex.platform.facility.dto;

/** 楼宇维保工单派单请求（③ S2）。assignee 必填。 */
public record FacilityWorkOrderAssignRequest(
        String assignee,
        String idempotencyKey
) {
}
