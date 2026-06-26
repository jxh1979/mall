package com.complex.platform.facility.dto;

import java.util.List;
import java.util.Map;

/** New building inspection template request. */
public record FacilityInspectionTemplateRequest(
        String name,
        String inspectionType,
        Integer cycleDays,
        String abnormalMode,
        List<Map<String, Object>> checklist,
        String scopeNote,
        Boolean active,
        String projectId,
        String idempotencyKey) {}
