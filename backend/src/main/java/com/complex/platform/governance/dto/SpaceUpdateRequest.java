package com.complex.platform.governance.dto;
// 文件版本：V0.01

import java.math.BigDecimal;

/**
 * 第十八批 BL 段第 388 条 388-R1（PGM:469「维护楼层、区域、点位、商铺……」；PGM:470「记录空间面积、用途、状态、负责人和所属商户。」）：
 * 修改空间的请求体。不含所属项目与平面图点位（修改时照原值）；status 不填按可用，填 unavailable 即停用。
 */
public record SpaceUpdateRequest(
        String idempotencyKey,
        String parentSpaceId,
        String spaceName,
        String spaceType,
        String floorNo,
        BigDecimal areaSquareMeter,
        String purpose,
        String ownerUserId,
        String merchantId,
        String status
) {
}
