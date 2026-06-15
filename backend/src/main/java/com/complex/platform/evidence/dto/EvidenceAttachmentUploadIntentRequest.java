package com.complex.platform.evidence.dto;

public record EvidenceAttachmentUploadIntentRequest(
        String idempotencyKey,
        String fileName,
        String contentType,
        Long fileSize,
        String businessType,
        String businessId
) {
}
