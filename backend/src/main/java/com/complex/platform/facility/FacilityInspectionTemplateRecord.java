package com.complex.platform.facility;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Building inspection template (S3, V167). */
public record FacilityInspectionTemplateRecord(
        String id,
        String name,
        String inspectionType,
        Integer cycleDays,
        String abnormalMode,
        List<Map<String, Object>> checklist,
        String scopeNote,
        boolean active,
        String projectId,
        Instant createdAt,
        Instant updatedAt) {}
