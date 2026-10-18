package com.complex.platform.dealer.dto;

import java.util.List;

public record DealerOrderDraftRequest(String idempotencyKey, String dealerContractId, List<DealerOrderLineRequest> lines, String note) {
}
