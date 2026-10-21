package com.complex.platform.ticketing.dto;

// 文件版本：V0.01

import java.util.List;

// TK-06（BSM:843「不设需要人脸照的票种」）：出票不再收人脸照与人脸同意。
public record TicketIssueRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String orderId,
        String issueMode,
        List<String> credentialTypes
) {
}
