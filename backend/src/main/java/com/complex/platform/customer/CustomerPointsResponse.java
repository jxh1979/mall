package com.complex.platform.customer;

import java.util.Map;

public record CustomerPointsResponse(
        String customerId,
        long balance,
        String level,
        Map<String, Object> recentSummary
) {
}
