package com.complex.platform.inventory;

// 文件版本：V0.01

import java.time.Instant;

/** 第十八批 M 段第 119 条 119-R1（BSM:1936「……库房、库位……都要先在基础管理中定义清楚。」）：库位主数据，id 即库位编号。 */
public record WarehouseLocationRecord(String id, String warehouseId, Instant createdAt) {
}
