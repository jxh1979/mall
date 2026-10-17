package com.complex.platform.dealer.dto;

import java.time.LocalDate;

public record DealerQuoteRequest(String idempotencyKey, Long quotedPriceCent, LocalDate quoteValidUntil, String note) {
}
