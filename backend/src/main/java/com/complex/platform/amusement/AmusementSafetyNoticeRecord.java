package com.complex.platform.amusement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

public record AmusementSafetyNoticeRecord(
        String id,
        String projectId,
        String amusementProjectId,
        String noticeType,
        String title,
        String contentVersion,
        String contentText,
        String status,
        Instant effectiveFrom,
        Instant effectiveTo,
        String createdByUserId,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt,
        @JsonIgnore
        String merchantId
) {
}
