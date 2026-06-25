package com.complex.platform.facility;

import java.time.Instant;
import java.util.List;

/** 楼宇巡检模板（facility_inspection_templates，V167，③ S3）。items=巡检项清单；equipmentType 空=通用。 */
public record FacilityInspectionTemplateRecord(
        String id,
        String name,
        String equipmentType,
        List<String> items,
        String status,
        String projectId,
        String merchantId,
        Instant createdAt,
        Instant updatedAt
) {
}
