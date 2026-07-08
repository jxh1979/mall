package com.complex.platform.inventory;

import java.time.Instant;

/** 平台级供货主体(酒水公司/中央餐饮等·非商户),拥有自己的供货库存,向商户供货并结算。V277。 */
public record SupplyEntityRecord(
        String id,
        String name,
        String entityType,
        String level,
        String status,
        String settlementAccount,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
