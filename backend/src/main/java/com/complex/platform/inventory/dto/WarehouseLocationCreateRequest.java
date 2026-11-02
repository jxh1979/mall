package com.complex.platform.inventory.dto;

// 文件版本：V0.01

/** 第十八批 M 段第 119 条 119-R1（BSM:1936）：在库房下建库位（idempotency_key、warehouse_id、location_id）。 */
public record WarehouseLocationCreateRequest(String idempotencyKey, String warehouseId, String locationId) {
}
