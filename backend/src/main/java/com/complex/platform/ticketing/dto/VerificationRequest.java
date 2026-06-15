package com.complex.platform.ticketing.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.Map;

public record VerificationRequest(
        String idempotencyKey,
        String credentialType,
        @JsonAlias({"entitlement_code", "entitlementCode"})
        String credentialPayload,
        String deviceId,
        String pointId,
        Map<String, Object> businessContext
) {
}
