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
        // V350 归一：登记不再接受前端传的 is_high_value / high_value_threshold_cents。
        // 「是不是高价值」由服务端按全平台唯一口径（物品进货价 vs 后台单一阈值 / 手工标记）现算，
        // 落库只作登记当刻的判定快照——改造前这两个字段由前端说了算，前端不传就永远低价值，
        // 存酒作废的主管复核形同虚设。
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
                null, null, null, null, null);
    }
}
