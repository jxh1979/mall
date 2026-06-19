package com.complex.platform.inventory.dto;

/**
 * R5 T6 建结构化库房请求。{@code warehouseType} 必填六类之一。
 */
public record WarehouseCreateRequest(
        String idempotencyKey,
        String projectId,
        String merchantId,
        String warehouseType,
        String name,
        String status
) {
}
