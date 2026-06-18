package com.complex.platform.common.api;

/**
 * Shared validation helpers for inbound API request fields. Centralizes guards
 * that were previously copy-pasted across every controller.
 */
public final class ApiRequests {

    private ApiRequests() {
    }

    public static void requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "idempotency_key is required");
        }
    }
}
