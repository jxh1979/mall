package com.complex.platform.governance.dto;

/** 商户存档/取消存档请求。仅需幂等键（存档动作无额外入参）。 */
public record MerchantArchiveRequest(String idempotencyKey) {
}
