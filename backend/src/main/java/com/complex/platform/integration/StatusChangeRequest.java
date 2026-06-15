package com.complex.platform.integration;

import java.util.List;

public record StatusChangeRequest(
        String idempotencyKey,
        String action,
        String reason,
        List<String> evidenceAttachmentIds
) {
}
