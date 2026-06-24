package com.complex.platform.leasing.dto;

/** 报价转合同请求（② 履约闭环 S4）：accepted 报价 → 生成合同 draft（喂入既有合同环），ops 在签约时补三主体。 */
public record LeasingConvertRequest(
        String contractSubjectId,
        String operationSubjectId,
        String settlementSubjectId,
        String idempotencyKey
) {
}
