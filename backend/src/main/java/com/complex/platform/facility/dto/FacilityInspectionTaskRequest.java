package com.complex.platform.facility.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** New building inspection task request. */
public record FacilityInspectionTaskRequest(
        String templateId,
        String inspectionType,
        String title,
        String scheduleMode,
        List<Map<String, Object>> routePoints,
        String assigneeId,
        LocalDate scheduledDate,
        LocalDate dueDate,
        String projectId,
        String spaceId,
        String idempotencyKey) {}
