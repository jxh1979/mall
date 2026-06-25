package com.complex.platform.facility;

import java.time.Instant;

/**
 * 楼宇维保/报修工单（facility_work_orders，V166，③ 楼宇运营 S2）。
 * equipmentId 可空（非设备类报修）；slaDueAt 建单时按 priority 推算；status: open|assigned|in_progress|resolved|closed。
 * 超期(overdue)按 slaDueAt 在读时派生，不落库。
 */
public record FacilityWorkOrderRecord(
        String id,
        String equipmentId,
        String spaceId,
        String title,
        String description,
        String category,
        String priority,
        String status,
        String assignee,
        String reportedBy,
        Instant slaDueAt,
        Instant resolvedAt,
        String projectId,
        String merchantId,
        Instant createdAt,
        Instant updatedAt
) {
}
