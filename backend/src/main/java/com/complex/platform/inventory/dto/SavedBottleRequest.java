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
        Long highValueThresholdCents,
        // P0#4 散客存酒登记扩展（openapi-p0.json F4/F7）：登记照片证据 + 人工备注。
        List<String> registrationPhotoRefs,
        String manualNote
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
                null, null, null, null, null, null, null);
    }

    /** 向后兼容构造器（R5 A1 签名，不含 P0#4 散客登记照片/人工备注）：既有调用方零改动。 */
    public SavedBottleRequest(
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
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, customerId, barId, items, expireAt,
                bottleLabelValue, customerNameOrNickname, customerPhone, isHighValue, highValueThresholdCents,
                null, null);
    }
}
