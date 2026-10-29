package com.complex.platform.inventory;

// 文件版本：V0.03

import java.time.Instant;
import java.util.List;

/**
 * 跨主体流转单:调拨(供货→商户·商户付货款)/借调(商户A→B·借=归还/调=买断)。V277。
 * FS-019（BSM L1201「不可消费物品（设备、器具等）的借用必须约定归还时间」）：借用单带 returnDueAt（应归还期限）。
 * 第十八批 G 段 82-R18a（第十八批裁决 18「要货单、借用单建单时记所属项目」）：projectId 为所属项目（V571）。
 */
public record InventoryTransferOrderRecord(
        String id,
        String transferType,
        String lendingMode,
        String fromOwnerType,
        String fromOwnerId,
        String toOwnerType,
        String toOwnerId,
        String fromLocationId,
        String toLocationId,
        String status,
        String operatorNote,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant returnDueAt,
        String projectId,
        List<InventoryTransferLineRecord> lines
) {
    /** 非借用单（无应归还期限）。 */
    public InventoryTransferOrderRecord(String id, String transferType, String lendingMode, String fromOwnerType, String fromOwnerId,
            String toOwnerType, String toOwnerId, String fromLocationId, String toLocationId, String status, String operatorNote,
            String createdBy, Instant createdAt, Instant updatedAt, List<InventoryTransferLineRecord> lines) {
        this(id, transferType, lendingMode, fromOwnerType, fromOwnerId, toOwnerType, toOwnerId, fromLocationId, toLocationId, status,
                operatorNote, createdBy, createdAt, updatedAt, null, null, lines);
    }
}
