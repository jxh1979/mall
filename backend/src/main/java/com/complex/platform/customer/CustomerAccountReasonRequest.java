package com.complex.platform.customer;

/** 拉黑/恢复客人账号请求（V194）。reason 进审计留痕。 */
public record CustomerAccountReasonRequest(String reason) {
}
