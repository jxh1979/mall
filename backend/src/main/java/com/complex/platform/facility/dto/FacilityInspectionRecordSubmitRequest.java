package com.complex.platform.facility.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Submit a building inspection record. */
public record FacilityInspectionRecordSubmitRequest(
        String result,
        List<String> photos,
        String qrCode,
        BigDecimal gpsLat,
        BigDecimal gpsLng,
        List<Map<String, Object>> checklistResult,
        String note,
        String abnormalNote,
        String inspectorId,
        String idempotencyKey) {}
