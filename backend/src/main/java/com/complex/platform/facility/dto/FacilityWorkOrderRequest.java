package com.complex.platform.facility.dto;

/** 新建楼宇维保/物业工单请求。 */
public record FacilityWorkOrderRequest(
        String workOrderType,
        String priority,
        String title,
        String description,
        String source,
        String spaceId,
        String equipmentId,
        String reporterId,
        String reporterType,
        String projectId,
        String merchantId,
        String idempotencyKey) {}
