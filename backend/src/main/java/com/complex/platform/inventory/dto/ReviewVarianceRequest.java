package com.complex.platform.inventory.dto;

/**
 * V108/V126 差异复核请求（pending → approved/rejected）。
 *
 * <p>{@code decision} 取 {@code approved}/{@code rejected}；幂等。approved 是解锁 finance
 * postInventoryVariance 入账的关键（finance 侧依赖 review_status='approved'）。{@code reason} 复核意见可空。
 */
public record ReviewVarianceRequest(
        String idempotencyKey,
        String decision,
        String reason
) {
}
