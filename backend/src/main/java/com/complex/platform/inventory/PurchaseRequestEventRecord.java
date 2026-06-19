package com.complex.platform.inventory;

import java.time.Instant;

/**
 * R5 T6 采购周期流转留痕事件（inventory_purchase_request_events）。
 *
 * <p>每次合法状态跳转写一行：from_status/to_status/operator_user_id/note，便于追责与回放。
 */
public record PurchaseRequestEventRecord(
        String id,
        String requestId,
        String fromStatus,
        String toStatus,
        String operatorUserId,
        String note,
        Instant createdAt
) {
}
