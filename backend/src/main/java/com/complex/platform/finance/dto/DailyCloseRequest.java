package com.complex.platform.finance.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyCloseRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        LocalDate businessDate,
        String scopeType,
        String scopeId,
        Boolean forceClose
) {
}
