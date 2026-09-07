package com.complex.platform.ticketing.dto;

import java.util.List;

public record StatusChangeRequest(
        String idempotencyKey,
        String action,
        String reason,
        List<String> evidenceAttachmentIds,
        // 结算重构批3-6(用户2026-07-04)：出票时(游乐票)携带的人脸照 payload(base64)列表；仅出票路径用，其余状态变更为空。
        List<String> facePhotos,
        // M009 A1-01（BSM-1.7.3）：facePhotos 非空时必填的客人同意留痕 id(customer_consents.id)；仅出票路径用。
        String faceConsentId
) {
    // ponytail: 向后兼容构造——63 处既有 4 参调用点不带 facePhotos，默认空列表(=不拍脸照)，零 churn。
    public StatusChangeRequest(String idempotencyKey, String action, String reason,
            List<String> evidenceAttachmentIds) {
        this(idempotencyKey, action, reason, evidenceAttachmentIds, List.of(), null);
    }

    // ponytail: 向后兼容构造——M009 前带 facePhotos 不带同意 id 的调用点。
    public StatusChangeRequest(String idempotencyKey, String action, String reason,
            List<String> evidenceAttachmentIds, List<String> facePhotos) {
        this(idempotencyKey, action, reason, evidenceAttachmentIds, facePhotos, null);
    }
}
