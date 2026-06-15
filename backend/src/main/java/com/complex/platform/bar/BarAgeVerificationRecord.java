package com.complex.platform.bar;

import java.time.Instant;
import java.util.List;

public record BarAgeVerificationRecord(
        String id,
        String businessId,
        String credentialPayload,
        String deviceId,
        String staffId,
        String result,
        String providerCode,
        String reviewStatus,
        List<String> evidenceAttachmentIds,
        Instant createdAt
) {
}
