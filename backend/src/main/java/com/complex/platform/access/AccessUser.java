package com.complex.platform.access;

import java.time.Instant;

public record AccessUser(
        String id,
        String username,
        String accountType,
        String organizationId,
        String merchantId,
        String status,
        Instant createdAt
) {
}
