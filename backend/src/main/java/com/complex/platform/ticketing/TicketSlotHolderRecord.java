package com.complex.platform.ticketing;

import java.time.Instant;

/**
 * AUD-077 人员权益组(slot)持有人。转赠单位=slot=(parent_ticket_id, slot_index)。
 * 出票时每 slot 一行(holder=购票人);转赠领取成功改受赠人。取码/我的票授权按此 holder(非订单归属)。
 */
public record TicketSlotHolderRecord(
        String parentTicketId,
        int slotIndex,
        String holderCustomerId,
        Instant createdAt,
        Instant updatedAt
) {
}
