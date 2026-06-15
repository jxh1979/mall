package com.complex.platform.governance.dto;

import java.time.LocalDate;
import java.util.List;

public record BusinessEntityBindingRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String merchantId,
        String operationEntityId,
        String contractEntityId,
        String settlementEntityId,
        LocalDate effectiveFrom,
        String changeReason
) {
}
