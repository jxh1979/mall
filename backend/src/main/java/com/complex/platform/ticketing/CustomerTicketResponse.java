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
        Instant validTo,
        // AUD-066：客人端"我的票"按父票聚合（一张卡片=票名+N人+剩余次数）。
        String parentTicketId,
        int slotIndex,
        String ticketNo
) {
    // ponytail: AUD-066 前不带 parent/slot/ticketNo 的调用点(测试)默认 null/0（未聚合父票）。
    public CustomerTicketResponse(String id, String ticketName, String entitlementType, String status,
            int totalTimes, int usedTimes, int remainingTimes, int numPeople, Instant validFrom, Instant validTo) {
        this(id, ticketName, entitlementType, status, totalTimes, usedTimes, remainingTimes, numPeople,
                validFrom, validTo, null, 0, null);
    }

    static CustomerTicketResponse of(IssuedEntitlementRecord record, String ticketName, String ticketNo) {
        return new CustomerTicketResponse(record.id(), ticketName, record.entitlementType(), record.status(),
                record.totalTimes(), record.usedTimes(), record.remainingTimes(), record.numPeople(),
                record.validFrom(), record.validTo(),
                record.parentTicketId(), record.slotIndex(), ticketNo);
    }
}
