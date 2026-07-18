package com.complex.platform.customer.dto;

/**
 * AUD-077 确认领取。link 渠道必带 claimCode 且须匹配;phone 渠道校验登录手机号盲索引=受赠手机号。
 */
public record CustomerTicketTransferClaimRequest(
        String idempotencyKey,
        String claimCode
) {
}
