package com.complex.platform.customer.dto;

/** CG-06a 通用卡充值赠送比例配置（按项目；万分比；须同时给出上下限）。projectId 为空 = 无项目范围的通用卡。 */
public record StoredValueBonusPolicyRequest(
        String projectId,
        Integer bonusBps,
        Integer minBps,
        Integer maxBps,
        String reason
) {
}
