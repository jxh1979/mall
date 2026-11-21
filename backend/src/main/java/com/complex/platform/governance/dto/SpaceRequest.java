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
        String status,
        String purpose,
        String ownerUserId
) {
    /** 第十八批 BL 段第 388 条 388-R1：补用途、负责人前的 12 项构造（两者为空），既有测试构造点不动。 */
    public SpaceRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String projectId,
            String merchantId, String parentSpaceId, String spaceName, String spaceType, String floorNo,
            List<Map<String, Object>> polygonPoints, BigDecimal areaSquareMeter, String status) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, projectId, merchantId, parentSpaceId, spaceName, spaceType, floorNo,
                polygonPoints, areaSquareMeter, status, null, null);
    }
}
