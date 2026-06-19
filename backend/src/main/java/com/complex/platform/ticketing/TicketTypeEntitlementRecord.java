package com.complex.platform.ticketing;

import java.time.Instant;

public record TicketTypeEntitlementRecord(
        String id,
        String ticketTypeId,
        String entitlementTemplateId,
        int quantity,
        int totalTimes,
        Long valueAmountCent,
        String effectiveRule,
        // R5 TODO#1：该票种该权益每份核销人数（binding 级）。联合票/多人票配 >1；默认 1。
        // 发放时写入 issued_entitlements.num_people，核销 verified_people 随之生效（接 R5 T5）。
        int numPeople,
        Instant createdAt,
        Instant updatedAt
) {
}
