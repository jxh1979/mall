package com.complex.platform.ticketing.dto;

// 文件版本：V0.03

import java.util.List;

// TK-06（BSM:843「不设需要人脸照的票种」）：出票等状态变更不再收人脸照与人脸同意。
public record StatusChangeRequest(
        String idempotencyKey,
        String action,
        String reason,
        List<String> evidenceAttachmentIds
) {
}
