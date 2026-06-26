package com.complex.platform.facility;

import java.time.Instant;

/** 楼宇维保/物业工单记录（S2，V166）。 */
public record FacilityWorkOrderRecord(
        String id,
        String code,
        String workOrderType,
        String priority,
        String status,
        String title,
        String description,
        String source,
        String spaceId,
        String equipmentId,
        String reporterId,
        String reporterType,
        String assigneeId,
        Instant responseDueAt,
        Instant handleDueAt,
        Instant assignedAt,
        Instant respondedAt,
        Instant completedAt,
        Instant verifiedAt,
        String verifierId,
        Integer satisfaction,
        boolean overtime,
        String resolution,
        String projectId,
        String merchantId,
        Instant createdAt,
        Instant updatedAt) {}
