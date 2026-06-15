package com.complex.platform.ticketing;

import java.time.Instant;

public record VerificationRecord(
        String id,
        String verificationNo,
        String entitlementId,
        String entitlementCode,
        String credentialType,
        String credentialPayload,
        String deviceId,
        String pointId,
        String status,
        String resultReason,
        String sourceType,
        String clientTaskId,
        Instant createdAt
) {
}
