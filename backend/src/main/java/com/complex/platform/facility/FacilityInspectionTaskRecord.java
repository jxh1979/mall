package com.complex.platform.facility;

import java.time.Instant;

/**
 * 楼宇巡检任务（facility_inspection_tasks，V167，③ S3）：按设备维保周期到期生成的待巡检待办。
 * status: pending|completed|overdue|cancelled；自然键 (equipmentId, dueAt) 防重。
 */
public record FacilityInspectionTaskRecord(
        String id,
        String equipmentId,
        String spaceId,
        Instant dueAt,
        String status,
        Instant generatedAt,
        String completedRecordId,
        Instant completedAt,
        String projectId,
        String merchantId,
        Instant createdAt,
        Instant updatedAt
) {
}
