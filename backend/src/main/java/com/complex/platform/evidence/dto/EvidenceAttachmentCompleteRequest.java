package com.complex.platform.evidence.dto;

public record EvidenceAttachmentCompleteRequest(
        String idempotencyKey,
        String objectKey,
        String checksum,
        Long fileSize
) {
}
