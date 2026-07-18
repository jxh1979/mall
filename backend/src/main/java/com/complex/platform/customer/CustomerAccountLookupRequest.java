package com.complex.platform.customer;

/**
 * AUD-020 按手机号精确查客人账号请求。
 *
 * <p>手机号属 PII，改走 POST 请求体传参（原 GET ?phone= 会把明文手机号落入 access log / 代理日志，违反 PIPL）。
 */
public record CustomerAccountLookupRequest(String phone) {
}
