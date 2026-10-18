package com.complex.platform.dealer.dto;

public record DealerReturnDecisionRequest(String idempotencyKey, String decision, String note) {
}
