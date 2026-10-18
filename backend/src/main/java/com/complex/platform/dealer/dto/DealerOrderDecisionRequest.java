package com.complex.platform.dealer.dto;

/** decision approve / reject。 */
public record DealerOrderDecisionRequest(String idempotencyKey, String decision, String note) {
}
