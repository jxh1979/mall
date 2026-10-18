package com.complex.platform.dealer.dto;

public record DealerPaymentVerifyRequest(String idempotencyKey, String decision, String note) {
}
