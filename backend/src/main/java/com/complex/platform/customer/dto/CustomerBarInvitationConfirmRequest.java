package com.complex.platform.customer.dto;

/**
 * V155 订卡邀请函——朋友在客人端确认接受（两步之第一步）。
 * invitationToken 来自转发的邀请函二维码；占用名额归确认人（customer token 解析的本人）。
 */
public record CustomerBarInvitationConfirmRequest(
        String idempotencyKey,
        String invitationToken
) {
}
