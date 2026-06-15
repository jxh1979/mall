package com.complex.platform.analytics;

import java.util.List;

public record StatusChangeRequest(
        String idempotencyKey,
        String action,
        String reason,
        List<String> evidenceAttachmentIds
) {
}
