package com.complex.platform.governance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ContractRecord(
        String id,
        String merchantId,
        String contractSubjectId,
        String operationSubjectId,
        String settlementSubjectId,
        LocalDate effectiveDate,
        LocalDate expiryDate,
        Map<String, Object> rentTerms,
        BigDecimal shareholderRatioForReport,
        List<String> attachmentIds,
        String lifecycleStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
