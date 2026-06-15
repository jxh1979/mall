package com.complex.platform.ticketing;

import java.util.List;

public record OfflinePackageResponse(
        String generatedAt,
        String expiresAt,
        List<OfflineEntitlementResponse> entitlements
) {
}
