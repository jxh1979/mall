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
        List<String> facePhotos,
        // M009 A1-01（BSM-1.7.3 采集人脸前单独取得同意并留同意版本、时间和范围）：facePhotos 非空时必填——
        //   客人在出票现场勾同意后落在同意台账(customer_consents)里的那条留痕 id；缺 → FACE_PHOTO_CONSENT_REQUIRED，
        //   对不上一条已生效的人脸照同意 → FACE_PHOTO_CONSENT_INVALID。请求不带照片时忽略。
        String faceConsentId
) {
    // ponytail: 向后兼容构造——批3-6 前的调用点(既有测试/issueOrder 旧构造)不带 facePhotos，默认空列表(=不拍脸照)。
    public TicketIssueRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds,
            String orderId, String issueMode, List<String> credentialTypes) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, orderId, issueMode, credentialTypes, List.of(), null);
    }

    // ponytail: 向后兼容构造——M009 前带 facePhotos 不带同意 id 的调用点；带照仍会在发放链被 FACE_PHOTO_CONSENT_REQUIRED 拒。
    public TicketIssueRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds,
            String orderId, String issueMode, List<String> credentialTypes, List<String> facePhotos) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, orderId, issueMode, credentialTypes, facePhotos, null);
    }
}
