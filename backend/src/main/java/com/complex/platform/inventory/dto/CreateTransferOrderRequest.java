package com.complex.platform.inventory.dto;

// 文件版本：V0.02

import java.time.Instant;
import java.util.List;

/** FS-019（BSM L1201「不可消费物品（设备、器具等）的借用必须约定归还时间」）：借用（lending+borrow）须带 returnDueAt。 */
public record CreateTransferOrderRequest(
        String idempotencyKey,
        String transferType,
        String lendingMode,
        String fromOwnerType,
        String fromOwnerId,
        String toOwnerType,
        String toOwnerId,
        String fromLocationId,
        String toLocationId,
        String operatorNote,
        List<TransferLineRequest> lines,
        Instant returnDueAt
) {
}
