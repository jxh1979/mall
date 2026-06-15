package com.complex.platform.integration;

public record OpenSignatureRequest(
        String appKey,
        Long timestamp,
        String nonce,
        String signature
) {
}
