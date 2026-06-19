package com.complex.platform.customer.dto;

/** 客人签到请求（每日一次，连续签到阶梯）。 */
public record CustomerCheckInRequest(
        String idempotencyKey,
        String sourceTerminal
) {
}
