package com.complex.platform.customer;

import java.time.Instant;

public record CustomerLoginResponse(
        String customerId,
        String maskedPhone,
        String customerToken,
        Instant expiresAt
) {
}
