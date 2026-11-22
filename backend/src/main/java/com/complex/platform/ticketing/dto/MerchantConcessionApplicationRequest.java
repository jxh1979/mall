package com.complex.platform.ticketing.dto;

// 文件版本：V0.01

/**
 * 第十八批 BV 段 264-R1（第十八批裁决 78「商户用自己的账号提交让利申请（哪张票、哪项权益）并在线确认」）：商户专区提交让利申请；
 * confirmed 须为 true（提交即在线确认）。
 */
public record MerchantConcessionApplicationRequest(
        String idempotencyKey,
        String ticketTypeId,
        String entitlementContent,
        Boolean confirmed
) {
}
