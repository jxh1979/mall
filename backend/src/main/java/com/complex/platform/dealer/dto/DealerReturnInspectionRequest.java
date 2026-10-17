package com.complex.platform.dealer.dto;

import java.util.List;

public record DealerReturnInspectionRequest(String idempotencyKey, List<DealerReturnInspectionLineRequest> lines, String note) {
}
