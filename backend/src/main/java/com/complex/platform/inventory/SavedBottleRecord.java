package com.complex.platform.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

public record SavedBottleRecord(
        String id,
        String customerId,
        String barProjectId,
        String itemId,
        String bottleName,
        int initialQuantity,
        int remainingQuantity,
        String unit,
        Instant expireAt,
        String status,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt,
        @JsonIgnore String merchantId,
        boolean isHighValue,
        Long highValueThresholdCents,
        String bottleLabelValue,
        String customerNameOrNickname,
        @JsonIgnore String customerPhone,
        @JsonIgnore String customerPhoneHash,
        // P0#4 散客登记扩展：登记照片证据 + 人工备注。
        List<String> registrationPhotoRefs,
        String manualNote,
        // P0#4 转赠受让信息：转赠时间 + 受让人姓名昵称/手机号/哈希。
        Instant transferredAt,
        String transferToNameOrNickname,
        @JsonIgnore String transferToPhone,
        @JsonIgnore String transferToPhoneHash
) {
    /** 向后兼容构造器（R5 A1 签名，不含 P0#4 登记/转赠扩展字段）：既有调用方零改动。 */
    public SavedBottleRecord(
            String id,
            String customerId,
            String barProjectId,
            String itemId,
            String bottleName,
            int initialQuantity,
            int remainingQuantity,
            String unit,
            Instant expireAt,
            String status,
            List<String> evidenceAttachmentIds,
            Instant createdAt,
            Instant updatedAt,
            String merchantId,
            boolean isHighValue,
            Long highValueThresholdCents,
            String bottleLabelValue,
            String customerNameOrNickname,
            String customerPhone,
            String customerPhoneHash
    ) {
        this(id, customerId, barProjectId, itemId, bottleName, initialQuantity, remainingQuantity, unit,
                expireAt, status, evidenceAttachmentIds, createdAt, updatedAt, merchantId, isHighValue,
                highValueThresholdCents, bottleLabelValue, customerNameOrNickname, customerPhone, customerPhoneHash,
                null, null, null, null, null, null);
    }
}
