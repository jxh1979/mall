package com.complex.platform.facility;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Building inspection submitted record (S3, V167). */
public record FacilityInspectionRecordRecord(
        String id,
        String taskId,
        String result,
        List<String> photos,
        String qrCode,
        BigDecimal gpsLat,
        BigDecimal gpsLng,
        List<Map<String, Object>> checklistResult,
        String note,
        String abnormalNote,
        String inspectorId,
        Instant recordedAt,
        Instant createdAt) {}
