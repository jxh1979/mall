package com.complex.platform.customer.dto;

// 文件版本：V0.01

/** 客人签到请求（每日一次，连续签到阶梯）。来源终端取本会话锁定的登录端（ROOT-03-R2），不收请求体。 */
public record CustomerCheckInRequest(
        String idempotencyKey
) {
}
