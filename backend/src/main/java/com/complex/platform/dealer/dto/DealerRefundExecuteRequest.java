package com.complex.platform.dealer.dto;

public record DealerRefundExecuteRequest(String idempotencyKey, String referenceNo) {
}
