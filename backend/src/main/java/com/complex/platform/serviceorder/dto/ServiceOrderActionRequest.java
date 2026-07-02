package com.complex.platform.serviceorder.dto;

public record ServiceOrderActionRequest(
        String idempotencyKey
) {
}
