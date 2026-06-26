package com.complex.platform.leasing.dto;

import java.time.LocalDate;

/**
 * 意向签约请求（V161 重建）：pending_signing 意向 → 转正式商户 + 喂入合同 draft。
 * 四类主体默认同一公司：仅必填 contractSubjectId，operation/settlement 为空时默认指向 contractSubjectId，后续可拆。
 * merchantId 为空时由签约自动建正式商户档案（带入线索/意向信息）。
 */
public record LeasingSignRequest(
        String merchantId,
        String contractSubjectId,
        String operationSubjectId,
        String settlementSubjectId,
        LocalDate effectiveDate,
        LocalDate expiryDate,
        String idempotencyKey
) {
}
