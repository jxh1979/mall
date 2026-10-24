package com.complex.platform.ticketing;

// 文件版本：V0.01

/**
 * AUD-077 领取预览(打开链接/二维码,不算领取):只读回票种概要,须登录且仅 pending 可预览。
 */
public record TicketTransferPreviewResponse(
        String transferId,
        String parentTicketId,
        int slotIndex,
        String channel,
        String status,
        String ticketName,
        int numPeople,
        int totalTimes,
        // clients-R40（BSM 第 809 行）：场次限制向受赠人明示；sessionEnded＝该人份权益所在场次已结束、权益已终结，不发生领取。
        boolean sessionRestricted,
        String sessionLabel,
        boolean sessionEnded
) {
}
