package com.complex.platform.access;

public record AccessGrantUserSummary(
        String userId,
        long requestCount,
        String summary
) {
}
