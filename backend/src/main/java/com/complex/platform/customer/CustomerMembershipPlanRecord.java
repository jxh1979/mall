package com.complex.platform.customer;

import java.time.Instant;
import java.util.Map;

/**
 * 付费会员档位（gap-analysis §7A P1：订阅制权益）。
 * priceCent=档位价格（分）；durationDays=权益时长（天）；benefits=权益 jsonb（折扣/赠饮等口径）。
 */
public record CustomerMembershipPlanRecord(
        String id,
        String projectId,
        String planCode,
        String name,
        long priceCent,
        int durationDays,
        Map<String, Object> benefits,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
