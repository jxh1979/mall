package com.complex.platform.customer.dto;

public record CustomerConsentRequest(
        String idempotencyKey,
        String consentType,
        Boolean accepted,
        String version,
        String sourceTerminal
) {
}
