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
        // M002-F27：幂等键列宽 VARCHAR(128)（OpenAPI maxLength 128）；超长的键在任何幂等预留与业务写入之前 400，不再落成列宽 500
        if (idempotencyKey.codePointCount(0, idempotencyKey.length()) > 128) {
            throw ApiException.badRequest("VALIDATION_FAILED", "幂等键不能超过 128 个字符");
        }
    }
}
