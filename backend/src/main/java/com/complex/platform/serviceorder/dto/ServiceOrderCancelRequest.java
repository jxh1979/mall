package com.complex.platform.serviceorder.dto;

public record ServiceOrderCancelRequest(
        String idempotencyKey,
        String reason
) {
}
