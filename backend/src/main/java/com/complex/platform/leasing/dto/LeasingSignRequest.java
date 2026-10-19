package com.complex.platform.leasing.dto;

// 文件版本：V0.01

import java.time.LocalDate;

/**
 * 意向签约请求（V161 重建）：pending_signing 意向 → 转正式商户 + 喂入合同 draft。
 * 四类主体默认同一公司：仅必填 contractSubjectId，operation/settlement 为空时默认指向 contractSubjectId，后续可拆。
 * merchantId 为空时由签约自动建正式商户档案（带入线索/意向信息）。
 * depositCent：第三轮裁决第 1 条「纯提成合同的押金由招商按合同约定金额直接录入」——纯提成意向签约时录入的约定押金（分），随签约喂入合同草稿，
 * 校验在合同域 createDraft；有月租的合同不填（押金按公式）。
 */
public record LeasingSignRequest(
        String merchantId,
        String contractSubjectId,
        String operationSubjectId,
        String settlementSubjectId,
        LocalDate effectiveDate,
        LocalDate expiryDate,
        String idempotencyKey,
        Long depositCent
) {
    /** 向后兼容构造（无 depositCent）。 */
    public LeasingSignRequest(String merchantId, String contractSubjectId, String operationSubjectId,
            String settlementSubjectId, LocalDate effectiveDate, LocalDate expiryDate, String idempotencyKey) {
        this(merchantId, contractSubjectId, operationSubjectId, settlementSubjectId, effectiveDate, expiryDate,
                idempotencyKey, null);
    }
}
