package com.complex.platform.contract.dto;

/** 提交合同进审批链（仅幂等键）。 */
public record ContractSubmitRequest(
        String idempotencyKey
) {
}
