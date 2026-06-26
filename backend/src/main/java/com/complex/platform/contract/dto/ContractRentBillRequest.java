package com.complex.platform.contract.dto;

/** 生成某期合同租金账单（按 ContractRentMath 算 base+递增−免租）。 */
public record ContractRentBillRequest(
        String idempotencyKey,
        String period           // YYYY-MM
) {
}
