package com.complex.platform.inventory;

import java.time.Instant;

/** 溯源码归属变更历史(append-only·防调包+结算对账)。V277。 */
public record InventoryTraceOwnershipHistoryRecord(
        String id,
        String traceCode,
        String fromOwnerType,
        String fromOwnerId,
        String toOwnerType,
        String toOwnerId,
        String transferOrderId,
        Instant changedAt
) {
}
