package com.complex.platform.governance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ContractDraftRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String merchantId,
        String contractSubjectId,
        String operationSubjectId,
        String settlementSubjectId,
        LocalDate effectiveDate,
        LocalDate expiryDate,
        Map<String, Object> rentTerms,
        BigDecimal shareholderRatioForReport,
        List<String> attachmentIds
) {
}
