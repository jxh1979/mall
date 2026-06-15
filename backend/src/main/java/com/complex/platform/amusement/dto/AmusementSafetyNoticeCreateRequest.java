package com.complex.platform.amusement.dto;

import java.time.Instant;
import java.util.List;

public record AmusementSafetyNoticeCreateRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String projectId,
        String amusementProjectId,
        String noticeType,
        String title,
        String contentVersion,
        String contentText,
        String status,
        Instant effectiveFrom,
        Instant effectiveTo,
        String operatorUserId
) {
}
