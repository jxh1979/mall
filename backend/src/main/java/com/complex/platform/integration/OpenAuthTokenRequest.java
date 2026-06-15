package com.complex.platform.integration;

public record OpenAuthTokenRequest(
        String appKey,
        Long timestamp,
        String nonce,
        String signature
) {
}
