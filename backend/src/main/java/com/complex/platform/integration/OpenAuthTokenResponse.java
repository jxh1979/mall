package com.complex.platform.integration;

import java.time.Instant;

public record OpenAuthTokenResponse(
        String token,
        String tokenType,
        Instant expiresAt
) {
}
