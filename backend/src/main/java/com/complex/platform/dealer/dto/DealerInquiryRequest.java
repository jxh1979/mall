package com.complex.platform.dealer.dto;

public record DealerInquiryRequest(String idempotencyKey, String dealerContractId, String itemId, String spec, Integer quantity, String note) {
}
