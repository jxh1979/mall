package com.complex.platform.ticketing;

import java.time.Instant;

public record IssuedEntitlementRecord(
        String id,
        String entitlementCode,
        String ticketTypeId,
        String orderId,
        String entitlementTemplateId,
        String entitlementType,
        String merchantId,
        String venueId,
        String pointId,
        int totalTimes,
        int usedTimes,
        Long valueAmountCent,
        String status,
        Instant validFrom,
        Instant validTo,
        Instant createdAt,
        Instant updatedAt,
        // R5 T5：发放时记录的核销人数（多人/联合票>1，普通票=1）。来源待票种配置（见 TicketOrderService TODO），当前默认 1。
        int numPeople
) {
    public int remainingTimes() {
        return totalTimes - usedTimes;
    }

    public IssuedEntitlementRecord useOnce(Instant updatedAt) {
        return new IssuedEntitlementRecord(
                id,
                entitlementCode,
                ticketTypeId,
                orderId,
                entitlementTemplateId,
                entitlementType,
                merchantId,
                venueId,
                pointId,
                totalTimes,
                usedTimes + 1,
                valueAmountCent,
                status,
                validFrom,
                validTo,
                createdAt,
                updatedAt,
                numPeople
        );
    }
}
