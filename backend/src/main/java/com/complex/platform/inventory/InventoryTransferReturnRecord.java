package com.complex.platform.inventory;

// 文件版本：V0.01

import java.time.Instant;
import java.util.List;

/** FS-019 借用归还登记（inventory_transfer_returns）：一次归还一行一条；receivedAt 即实际归还时间（借出方收货扫码时刻）。 */
public record InventoryTransferReturnRecord(
        String id,
        String returnNo,
        String transferOrderId,
        String lineId,
        int returnedQuantity,
        List<String> traceCodes,
        String receivedBy,
        Instant receivedAt
) {
}
