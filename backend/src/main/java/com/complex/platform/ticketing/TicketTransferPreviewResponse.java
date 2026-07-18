package com.complex.platform.ticketing;

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
        int totalTimes
) {
}
