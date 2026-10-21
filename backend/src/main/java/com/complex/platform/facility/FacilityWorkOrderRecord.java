package com.complex.platform.facility;

// 文件版本：V0.01

import java.time.Instant;

/**
 * 楼宇维保/物业工单记录（S2，V166）。
 * HP-F02（V531）：verifiedAt/verifierId＝主管／物业验收事实；reporterConfirmedAt/By＝提单方（提单确认人）确认事实；
 * confirmerId＝自动生成的大型维修工单的提单确认承接人（设备在岗责任人，或物业主管指定的其他人）。
 */
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
        Instant updatedAt,
        String confirmerId,
        Instant reporterConfirmedAt,
        String reporterConfirmedBy) {}
