package com.complex.platform.evidence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

public record EvidenceAttachmentRecord(
        String id,
        String fileName,
        String contentType,
        Long fileSize,
        String businessType,
        String businessId,
        String attachmentRole,
        String objectKey,
        String uploadUrl,
        String status,
        String checksum,
        Instant createdAt,
        Instant updatedAt,
        @JsonIgnore String merchantId
) {
}
