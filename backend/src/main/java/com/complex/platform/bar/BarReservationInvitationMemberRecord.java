package com.complex.platform.bar;

import java.time.Instant;

/**
 * V155 订卡邀请函成员（bar_reservations 子记录）：持函人与每个确认接受邀请的朋友各一行。
 * 两步语义：confirm（朋友在客人端确认接受，占一个名额）→ entered（到店扫码核验入场）。
 * 「已确认 + 已入场」总人数 ≤ 邀请函 capacity（卡座可进人数）。
 */
public record BarReservationInvitationMemberRecord(
        String id,
        String reservationId,
        String barId,
        String memberIdentifier,
        String memberCustomerId,
        boolean isHolder,
        String status,
        Instant confirmedAt,
        Instant enteredAt,
        String deviceId,
        Instant createdAt,
        Instant updatedAt
) {
}
