package com.complex.platform.finance.dto;

import java.util.List;

/**
 * ② 履约闭环 S2 合同租金按期计费：对指定 contractIds 按 rent_terms.baseRentCent 生成当期租金应收。
 * 仅 effective 合同 + 有 baseRentCent 的合同入账；(合同,期) 已计过则跳过（幂等，不重复挂账）。
 * 功能标志 contract.rent_billing.enabled 默认关。
 */
public record ContractRentBillRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String period,
        List<String> contractIds
) {
}
