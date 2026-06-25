package com.complex.platform.facility.dto;

import java.util.List;

/** 楼宇巡检模板建档请求（③ S3）。name 必填；items 巡检项清单；equipmentType 可空（通用）。 */
public record FacilityInspectionTemplateRequest(
        String name,
        String equipmentType,
        List<String> items,
        String idempotencyKey
) {
}
