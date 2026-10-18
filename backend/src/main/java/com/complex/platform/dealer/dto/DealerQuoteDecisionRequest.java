package com.complex.platform.dealer.dto;

/** decision approve / reject。 */
public record DealerQuoteDecisionRequest(String idempotencyKey, String decision, String note) {
}
