package com.complex.platform.dealer.dto;

public record DealerDeactivateRequest(String idempotencyKey, String reason) {
}
