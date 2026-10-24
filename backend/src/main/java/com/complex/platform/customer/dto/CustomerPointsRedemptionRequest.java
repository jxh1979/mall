package com.complex.platform.customer.dto;

// 文件版本：V0.01

/** 积分兑换请求。来源终端取本会话锁定的登录端（ROOT-03-R2），不收请求体。 */
public record CustomerPointsRedemptionRequest(
        String idempotencyKey,
        String catalogItemId,
        Integer quantity
) {
}
