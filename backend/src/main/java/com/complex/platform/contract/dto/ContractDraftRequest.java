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
        String businessScope,
        // 3.13：草稿期意向空间（可选）。传了则起草即校验空间存在 + 与生效合同租期重叠早拒，并预绑定 contract_spaces。
        List<String> spaceIds,
        // 经营范围（结构化，可空）：取值=既有业务类目规范值（ContractScopeGuard.ALLOWED_CATEGORY_VALUES），
        // 存 rent_terms.allowedCategories；配置后下单侧（酒吧/票务）拦超范围经营。空/缺省=不拦截（存量兼容）。
        List<String> allowedCategories
) {
    /** 向后兼容构造（无 allowedCategories）。 */
    public ContractDraftRequest(
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
            String businessScope,
            List<String> spaceIds) {
        this(idempotencyKey, merchantId, contractSubjectId, operationSubjectId, settlementSubjectId,
                effectiveDate, expiryDate, rentTerms, commissionMode, energyFeeBasis, depositCent,
                penaltyTerms, settlementRules, attachmentIds, projectId, businessScope, spaceIds, null);
    }

    /** 向后兼容构造（无 spaceIds / allowedCategories）。 */
    public ContractDraftRequest(
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
            String businessScope) {
        this(idempotencyKey, merchantId, contractSubjectId, operationSubjectId, settlementSubjectId,
                effectiveDate, expiryDate, rentTerms, commissionMode, energyFeeBasis, depositCent,
                penaltyTerms, settlementRules, attachmentIds, projectId, businessScope, null, null);
    }
}
