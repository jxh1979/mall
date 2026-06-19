package com.complex.platform.customer;

import java.time.Instant;
import java.time.LocalDate;

public record CustomerRecord(
        String id,
        String phone,
        String maskedPhone,
        String status,
        LocalDate birthDate,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {
}
