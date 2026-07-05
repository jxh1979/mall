package com.complex.platform.ticketing.dto;

import java.util.List;

public record StatusChangeRequest(
        String idempotencyKey,
        String action,
        String reason,
        List<String> evidenceAttachmentIds,
        // 结算重构批3-6(用户2026-07-04)：出票时(游乐票)携带的人脸照 payload(base64)列表；仅出票路径用，其余状态变更为空。
        List<String> facePhotos
) {
    // ponytail: 向后兼容构造——63 处既有 4 参调用点不带 facePhotos，默认空列表(=不拍脸照)，零 churn。
    public StatusChangeRequest(String idempotencyKey, String action, String reason,
            List<String> evidenceAttachmentIds) {
        this(idempotencyKey, action, reason, evidenceAttachmentIds, List.of());
    }
}
