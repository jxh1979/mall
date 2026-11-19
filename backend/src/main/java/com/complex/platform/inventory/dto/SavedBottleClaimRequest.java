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
        // 不再读取（BSM:1073 只核验短信验证码或动态取酒码）；契约已删这两个字段，记录组件保留免改既有构造。
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
