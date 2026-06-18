package com.complex.platform.floorplan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

public record FloorPlanRecord(
        String id,
        String projectId,
        String buildingCode,
        String floorNo,
        String planName,
        String imageUrl,
        Integer imageWidth,
        Integer imageHeight,
        String versionNo,
        String status,
        Instant createdAt,
        Instant updatedAt,
        @JsonIgnore String merchantId
) {
}
