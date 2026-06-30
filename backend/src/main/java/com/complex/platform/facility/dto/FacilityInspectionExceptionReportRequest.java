package com.complex.platform.facility.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Report an exception on an assigned building inspection task. */
public record FacilityInspectionExceptionReportRequest(
        List<String> photos,
        String qrCode,
        BigDecimal gpsLat,
        BigDecimal gpsLng,
        List<Map<String, Object>> checklistResult,
        String note,
        String abnormalNote,
        String inspectorId,
        String idempotencyKey) {}
