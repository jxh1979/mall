package com.complex.platform.facility;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Building inspection task (S3, V167). */
public record FacilityInspectionTaskRecord(
        String id,
        String templateId,
        String inspectionType,
        String status,
        String title,
        String scheduleMode,
        List<Map<String, Object>> routePoints,
        String assigneeId,
        LocalDate scheduledDate,
        LocalDate dueDate,
        Instant startedAt,
        Instant completedAt,
        String workOrderId,
        String projectId,
        String spaceId,
        Instant createdAt,
        Instant updatedAt) {}
