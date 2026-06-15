package com.complex.platform.ticketing;

public record VerificationPreviewResponse(
        boolean allowed,
        String reason,
        OfflineEntitlementResponse entitlement
) {
}
