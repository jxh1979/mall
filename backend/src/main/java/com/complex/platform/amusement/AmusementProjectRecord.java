package com.complex.platform.amusement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.Map;

public record AmusementProjectRecord(
        String id,
        String projectName,
        int capacity,
        String openStatus,
        String safetyTemplateId,
        Map<String, Object> eligibilityRule,
        // V114 清单#21：项目入口点位（可空；未绑定为 NULL，向后兼容）。
        String locationPointId,
        Instant createdAt,
        Instant updatedAt,
        @JsonIgnore
        String merchantId
) {
}
