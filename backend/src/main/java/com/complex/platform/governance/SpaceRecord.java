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
        Instant updatedAt,
        String purpose,
        String ownerUserId
) {
    /** 第十八批 BL 段第 388 条 388-R1（PGM:470 用途、负责人）：补两项前的 12 项构造，两者为空。 */
    public SpaceRecord(String id, String projectId, String merchantId, String parentSpaceId, String spaceName, String spaceType,
            String floorNo, BigDecimal areaSquareMeter, List<Map<String, Object>> polygonPoints, String status,
            Instant createdAt, Instant updatedAt) {
        this(id, projectId, merchantId, parentSpaceId, spaceName, spaceType, floorNo, areaSquareMeter, polygonPoints, status,
                createdAt, updatedAt, null, null);
    }
}
