package com.complex.platform.dealer.dto;

/** decision approve / reject。 */
public record DealerCatalogDecisionRequest(String idempotencyKey, String decision, String note) {
}
