package com.complex.platform.ticketing;

import java.time.Instant;

/**
 * AUD-077 转赠安全投影:只出可见字段。受赠手机号密文/盲索引绝不出网(仅掩码);
 * claim_code 仅回给发起人本人(创建结果 / 我发起的列表),受赠视角与后台只读均置 null。
 */
public record TicketTransferResponse(
        String id,
        String parentTicketId,
        int slotIndex,
        String channel,
        String status,
        String claimCode,
        String receiverPhoneMasked,
        String ticketName,
        String voidReason,
        Instant createdAt,
        Instant claimedAt
) {
    static TicketTransferResponse of(TicketTransferRecord r, String ticketName, boolean includeClaimCode) {
        return new TicketTransferResponse(
                r.id(), r.parentTicketId(), r.slotIndex(), r.channel(), r.status(),
                includeClaimCode ? r.claimCode() : null,
                r.receiverPhoneMasked(), ticketName, r.voidReason(), r.createdAt(), r.claimedAt());
    }
}
