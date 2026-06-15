package com.complex.platform.bar;

import java.util.List;

public record BarTableRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String barId,
        String tableName,
        String tableType,
        Long minimumSpendCent,
        Integer allowedFreeEntryCount,
        String status
) {
}
