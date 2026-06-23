package com.complex.platform.bar;

import java.time.Instant;
import java.util.List;

/**
 * V155 订卡邀请函视图（签发/查询响应）：承载二维码所需的 token + 防伪签名 + 卡座/容量，以及当前占用计数与成员明细。
 * confirmedCount = 已确认（含未入场）成员数；enteredCount = 已入场成员数；remaining = capacity − confirmedCount。
 * 客人端据 token/signature 生成可转发二维码；门口据成员状态核验入场。
 */
public record BarReservationInvitationRecord(
        String reservationId,
        String barId,
        String tableId,
        String invitationToken,
        String invitationSignature,
        int capacity,
        String status,
        Instant issuedAt,
        int confirmedCount,
        int enteredCount,
        int remaining,
        List<BarReservationInvitationMemberRecord> members
) {
}
