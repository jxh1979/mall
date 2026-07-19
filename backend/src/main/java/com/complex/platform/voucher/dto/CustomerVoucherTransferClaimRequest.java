package com.complex.platform.voucher.dto;

/**
 * V313 确认领取券转赠。link 渠道必带 claimCode 且须匹配；phone 渠道校验登录手机号盲索引=受赠手机号。
 */
public record CustomerVoucherTransferClaimRequest(
        String idempotencyKey,
        String claimCode
) {
}
