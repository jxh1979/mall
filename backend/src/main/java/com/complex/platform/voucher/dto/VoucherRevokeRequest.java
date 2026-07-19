package com.complex.platform.voucher.dto;

/** 作废券请求：理由必填（审计留痕）。 */
public record VoucherRevokeRequest(
        String idempotencyKey,
        String reason
) {
}
