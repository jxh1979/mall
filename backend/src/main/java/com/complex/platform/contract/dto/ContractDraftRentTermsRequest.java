package com.complex.platform.contract.dto;

// 文件版本：V0.01

import java.util.Map;

/**
 * 第四轮裁决第 4 条「合同草稿可编辑补录」：草拟中合同补录/修改租金条款（rentTerms，与起草同口径并入）；
 * depositCent 只给纯提成合同（约定押金，大于 0），有月租的合同押金按公式重算、不收手工值。
 */
public record ContractDraftRentTermsRequest(
        String idempotencyKey,
        Map<String, Object> rentTerms,
        Long depositCent
) {
}
