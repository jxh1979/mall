package com.complex.platform.dealer.dto;

import java.util.List;

public record DealerCatalogDraftRequest(String idempotencyKey, String note, List<DealerCatalogLineRequest> lines) {
}
