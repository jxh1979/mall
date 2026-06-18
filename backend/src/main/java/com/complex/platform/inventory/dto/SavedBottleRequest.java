package com.complex.platform.inventory.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SavedBottleRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String customerId,
        String barId,
        List<Map<String, Object>> items,
        Instant expireAt,
        String bottleLabelValue,
        String customerNameOrNickname,
        String customerPhone,
        Boolean isHighValue,
        Long highValueThresholdCents
) {
    /** 向后兼容构造器（不含 A1 存酒登记扩展字段 REQ-30/32）：既有调用方零改动。 */
    public SavedBottleRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String customerId,
            String barId,
            List<Map<String, Object>> items,
            Instant expireAt
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, customerId, barId, items, expireAt,
                null, null, null, null, null);
    }
}
