package com.complex.platform.bar;

import java.time.Instant;
import java.util.List;

public record BarProductionTicketEventRecord(
        String id,
        String ticketId,
        String orderId,
        String barId,
        String actionType,
        String fromStatus,
        String toStatus,
        String stationCode,
        String operatorUserId,
        String operatorNote,
        String idempotencyKey,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        // R5 T15：出品 deliver 实际扣库存后回填的 inventory_stock_movements / 库存流水 id（修 R4-G1 events 无库存关联）。
        String inventoryMovementId
) {
    /** 向后兼容构造：未指定库存流水关联的既有调用默认 null。 */
    public BarProductionTicketEventRecord(
            String id,
            String ticketId,
            String orderId,
            String barId,
            String actionType,
            String fromStatus,
            String toStatus,
            String stationCode,
            String operatorUserId,
            String operatorNote,
            String idempotencyKey,
            List<String> evidenceAttachmentIds,
            Instant createdAt
    ) {
        this(id, ticketId, orderId, barId, actionType, fromStatus, toStatus, stationCode,
                operatorUserId, operatorNote, idempotencyKey, evidenceAttachmentIds, createdAt, null);
    }
}
