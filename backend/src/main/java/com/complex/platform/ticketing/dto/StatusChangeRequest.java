package com.complex.platform.ticketing.dto;

import java.util.List;

public record StatusChangeRequest(
        String idempotencyKey,
        String action,
        String reason,
        List<String> evidenceAttachmentIds
) {
}
