package com.complex.platform.integration;

public record WebhookAcceptedResponse(
        String eventId,
        boolean deduplicated
) {
}
