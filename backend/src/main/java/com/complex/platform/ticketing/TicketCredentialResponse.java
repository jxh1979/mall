package com.complex.platform.ticketing;

import java.time.Instant;

public record TicketCredentialResponse(
        String id,
        String ticketId,
        String orderId,
        String credentialToken,
        String credentialType,
        Instant issuedAt,
        Instant expiresAt,
        int refreshedCount
) {
}
