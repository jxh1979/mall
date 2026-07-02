package com.complex.platform.serviceorder;

import java.time.Instant;

/** G批:服务单——下单即指定履约人(口径③);价格为目录快照参考价(口径④直付,不入账)。 */
public record ServiceOrderRecord(
        String id,
        String catalogItemId,
        String itemName,
        long priceCent,
        String barId,
        String tableId,
        String performerUserId,
        String orderedBy,
        String customerNote,
        String status,
        String cancelReason,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {
}
