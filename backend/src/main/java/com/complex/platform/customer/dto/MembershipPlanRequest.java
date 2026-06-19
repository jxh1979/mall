package com.complex.platform.customer.dto;

import java.util.Map;

/** 付费会员档位配置请求（后台）。 */
public record MembershipPlanRequest(
        String projectId,
        String planCode,
        String name,
        Long priceCent,
        Integer durationDays,
        Map<String, Object> benefits,
        String status
) {
}
