package com.complex.platform.serviceorder.dto;

public record ServicePayConfigRequest(
        String idempotencyKey,
        String qrContent
) {
}
