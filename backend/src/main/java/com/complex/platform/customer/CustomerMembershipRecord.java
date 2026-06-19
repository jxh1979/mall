package com.complex.platform.customer;

import java.time.Instant;

/**
 * 付费会员订阅（gap-analysis §7A P1）。
 * status=active|expired|cancelled；start/expire=生效窗口；paymentIntentId=开通关联支付意图；
 * autoRenew=自动续费意向（实际扣款待支付渠道）；idempotencyKey 唯一防重复开通。
 */
public record CustomerMembershipRecord(
        String id,
        String customerId,
        String planId,
        String projectId,
        Instant startAt,
        Instant expireAt,
        String status,
        boolean autoRenew,
        String paymentIntentId,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt
) {
}
