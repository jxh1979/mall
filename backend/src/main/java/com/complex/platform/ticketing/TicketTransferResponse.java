package com.complex.platform.ticketing;

// 文件版本：V0.01

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
        Instant claimedAt,
        // clients-R40（BSM 第 809 行「转出时，系统须同时向转出人与受赠人明示这一场次限制」）：多人票同票已有人入场＝本人份只能在该场次内入场。
        boolean sessionRestricted,
        String sessionLabel,
        boolean sessionEnded
) {
    static TicketTransferResponse of(TicketTransferRecord r, String ticketName, boolean includeClaimCode, FirstSessionWindow.Limit limit) {
        return new TicketTransferResponse(
                r.id(), r.parentTicketId(), r.slotIndex(), r.channel(), r.status(),
                includeClaimCode ? r.claimCode() : null,
                r.receiverPhoneMasked(), ticketName, r.voidReason(), r.createdAt(), r.claimedAt(),
                limit.restricted(), limit.label(), limit.ended());
    }
}
