package com.complex.platform.governance.dto;

import java.util.List;

public record ProjectRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String merchantId,
        String venueType,
        String name,
        String code,
        String floor,
        Integer capacity,
        String status
) {
}
