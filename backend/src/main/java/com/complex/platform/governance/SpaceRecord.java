package com.complex.platform.governance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SpaceRecord(
        String id,
        String projectId,
        String merchantId,
        String parentSpaceId,
        String spaceName,
        String spaceType,
        String floorNo,
        BigDecimal areaSquareMeter,
        List<Map<String, Object>> polygonPoints,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
