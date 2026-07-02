package com.complex.platform.ticketing;

import java.time.Instant;

/**
 * 1.8 客人端"我的票"安全投影：只给客人可见字段；
 * 内部分账（allocated_revenue_cent）、商户/场地内部 id、完整核销码均不出网。
 */
public record CustomerTicketResponse(
        String id,
        String ticketName,
        String entitlementType,
        String status,
        int totalTimes,
        int usedTimes,
        int remainingTimes,
        int numPeople,
        Instant validFrom,
        Instant validTo
) {
    static CustomerTicketResponse of(IssuedEntitlementRecord record, String ticketName) {
        return new CustomerTicketResponse(record.id(), ticketName, record.entitlementType(), record.status(),
                record.totalTimes(), record.usedTimes(), record.remainingTimes(), record.numPeople(),
                record.validFrom(), record.validTo());
    }
}
