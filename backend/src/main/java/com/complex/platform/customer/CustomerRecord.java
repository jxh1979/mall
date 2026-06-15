package com.complex.platform.customer;

import java.time.Instant;

public record CustomerRecord(
        String id,
        String phone,
        String maskedPhone,
        String status,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {
}
