package com.complex.platform.voucher.dto;

/** 统一核销请求：按核销码或实例 id 二选一定位券。 */
public record VoucherRedeemRequest(
        String idempotencyKey,
        String code,
        String voucherInstanceId,
        String note
) {
}
