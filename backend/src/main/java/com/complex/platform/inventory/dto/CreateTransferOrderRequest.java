package com.complex.platform.inventory.dto;

// 文件版本：V0.03

import java.time.Instant;
import java.util.List;

/**
 * FS-019（BSM L1201「不可消费物品（设备、器具等）的借用必须约定归还时间」）：借用（lending+borrow）须带 returnDueAt。
 * 第十八批 G 段 82-R18a（第十八批裁决 18「在 m 的经营项目里选，只有一个时自动带」）：projectId 为所属项目，接收方商户只有一个经营项目时可不带。
 */
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
        Instant returnDueAt,
        String projectId
) {
}
