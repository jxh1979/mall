package com.complex.platform.dealer.dto;

public record DealerCancelRequest(String idempotencyKey, String reason) {
}
