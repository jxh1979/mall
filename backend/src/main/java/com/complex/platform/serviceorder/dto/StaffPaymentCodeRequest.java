package com.complex.platform.serviceorder.dto;

public record StaffPaymentCodeRequest(
        String idempotencyKey,
        String qrContent
) {
}
