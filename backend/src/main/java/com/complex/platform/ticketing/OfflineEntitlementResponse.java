package com.complex.platform.ticketing;

public record OfflineEntitlementResponse(
        String id,
        String entitlementCode,
        String entitlementType,
        String merchantId,
        String venueId,
        String projectId,
        int totalTimes,
        int usedTimes,
        Long valueAmount,
        String status,
        String validFrom,
        String validTo,
        String updatedAt
) {
}
