package com.complex.platform.governance.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record SpaceRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String projectId,
        String merchantId,
        String parentSpaceId,
        String spaceName,
        String spaceType,
        String floorNo,
        List<Map<String, Object>> polygonPoints,
        BigDecimal areaSquareMeter,
        String status
) {
}
