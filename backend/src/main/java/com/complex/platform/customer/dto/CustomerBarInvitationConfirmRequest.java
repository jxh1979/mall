package com.complex.platform.customer.dto;

// 文件版本：V0.01

/**
 * V155 订卡邀请函——朋友在客人端确认接受（两步之第一步）。
 * invitationToken 来自转发的邀请函二维码；占用名额归确认人（customer token 解析的本人）。
 * ROOT-03-R25（BSM 第 1036 行「邀请函二维码带防伪标记，被复制或篡改卡座、可进人数即无法接受」）：二维码载荷的防伪标记、卡座与可进人数一并回传核验。
 */
public record CustomerBarInvitationConfirmRequest(
        String idempotencyKey,
        String invitationToken,
        String invitationSignature,
        String tableId,
        Integer capacity
) {
}
