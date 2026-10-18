package com.complex.platform.dealer.dto;

public record DealerShipRequest(String idempotencyKey, String note) {
}
