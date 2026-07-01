package com.complex.platform.customer;

/** 平台封禁客人账号请求（V194）。suspendedUntil=ISO-8601 封禁到期日(到期自动恢复)；reason 进审计留痕。 */
public record CustomerSuspendRequest(String suspendedUntil, String reason) {
}
