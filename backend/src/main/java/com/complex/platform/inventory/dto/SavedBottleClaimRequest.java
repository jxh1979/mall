package com.complex.platform.inventory.dto;

import java.util.List;
import java.util.Map;

public record SavedBottleClaimRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        List<Map<String, Object>> claimItems,
        String claimByPhone,
        String verificationCode,
        // P0#4 取酒核身（openapi-p0.json F4/F7）：标签值 + 姓名/昵称强制比对项。
        String bottleLabelValue,
        String customerNameOrNickname
) {
    /** 向后兼容构造器（R5 签名，不含 P0#4 取酒核身比对项）：既有调用方零改动。 */
    public SavedBottleClaimRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            List<Map<String, Object>> claimItems,
            String claimByPhone,
            String verificationCode
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, claimItems, claimByPhone, verificationCode,
                null, null);
    }
}
