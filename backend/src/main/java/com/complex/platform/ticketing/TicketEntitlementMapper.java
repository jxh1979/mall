package com.complex.platform.ticketing;

final class TicketEntitlementMapper {

    private TicketEntitlementMapper() {
    }

    static OfflineEntitlementResponse toOfflineEntitlementResponse(IssuedEntitlementRecord entitlement) {
        return new OfflineEntitlementResponse(
                entitlement.id(),
                entitlement.entitlementCode(),
                entitlement.entitlementType(),
                entitlement.merchantId(),
                entitlement.venueId(),
                entitlement.pointId(),
                entitlement.totalTimes(),
                entitlement.usedTimes(),
                entitlement.valueAmountCent(),
                entitlement.status(),
                entitlement.validFrom().toString(),
                entitlement.validTo().toString(),
                entitlement.updatedAt().toString()
        );
    }
}
