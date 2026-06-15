package com.complex.platform.evidence.dto;

public record EvidenceAttachmentBindRequest(
        String idempotencyKey,
        String businessType,
        String businessId,
        String attachmentRole
) {
}
