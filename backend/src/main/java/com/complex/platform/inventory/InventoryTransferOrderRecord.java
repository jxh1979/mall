package com.complex.platform.inventory;

import java.time.Instant;
import java.util.List;

/** 跨主体流转单:调拨(供货→商户·商户付货款)/借调(商户A→B·借=归还/调=买断)。V277。 */
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
        List<InventoryTransferLineRecord> lines
) {
}
