package com.complex.platform.facility.dto;

/** 楼宇维保工单建单请求（③ S2）。category 必填且在枚举内；priority 可空（默认 medium）；equipmentId/spaceId 可空。 */
public record FacilityWorkOrderRequest(
        String equipmentId,
        String spaceId,
        String title,
        String description,
        String category,
        String priority,
        String reportedBy,
        String idempotencyKey
) {
}
