package com.complex.platform.dealer.dto;

public record DealerInvoiceRequest(String idempotencyKey, String note) {
}
