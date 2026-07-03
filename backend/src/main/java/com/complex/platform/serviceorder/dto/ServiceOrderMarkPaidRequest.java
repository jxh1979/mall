package com.complex.platform.serviceorder.dto;

public record ServiceOrderMarkPaidRequest(
        String idempotencyKey,
        String payMethod
) {
}
