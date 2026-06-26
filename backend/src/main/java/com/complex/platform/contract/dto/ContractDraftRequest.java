package com.complex.platform.contract.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 起草合同（手工 或 招商签约带入）。租金/递增/免租/物业费放 rentTerms。 */
public record ContractDraftRequest(
        String idempotencyKey,
        String merchantId,
        String contractSubjectId,
        String operationSubjectId,
        String settlementSubjectId,
        LocalDate effectiveDate,
        LocalDate expiryDate,
        Map<String, Object> rentTerms,
        String commissionMode,
        String energyFeeBasis,
        Long depositCent,
        Map<String, Object> penaltyTerms,
        List<Map<String, Object>> settlementRules,
        List<String> attachmentIds,
        String projectId,
        String businessScope
) {
}
