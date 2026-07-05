package com.complex.platform.ticketing.dto;

import java.util.List;

public record TicketIssueRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String orderId,
        String issueMode,
        List<String> credentialTypes,
        // 结算重构批3-6(用户2026-07-04)：人脸照 payload(base64)列表，按需拍脸照的权益按发放顺序逐张取用。
        // require_face_photo=TRUE 的权益缺对应照 → 拒 FACE_PHOTO_REQUIRED；FALSE 的票此表为空、零影响。
        List<String> facePhotos
) {
    // ponytail: 向后兼容构造——批3-6 前的调用点(既有测试/issueOrder 旧构造)不带 facePhotos，默认空列表(=不拍脸照)。
    public TicketIssueRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds,
            String orderId, String issueMode, List<String> credentialTypes) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, orderId, issueMode, credentialTypes, List.of());
    }
}
