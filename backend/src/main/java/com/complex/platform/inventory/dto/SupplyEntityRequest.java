package com.complex.platform.inventory.dto;

/** 创建/更新供货主体请求。 */
public record SupplyEntityRequest(
        String idempotencyKey,
        String name,
        String entityType,
        String settlementAccount,
        String status
) {
}
