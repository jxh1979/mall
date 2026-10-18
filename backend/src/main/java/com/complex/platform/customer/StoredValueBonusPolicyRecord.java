package com.complex.platform.customer;

import java.time.Instant;

/**
 * CG-06a 综合体通用卡充值赠送比例（ENUM「储值卡类型与收款主体」：「赠送比例可配（业务参数，按项目可配，须同时配置上下限）」）。
 * projectId 为综合体项目 id，{@link #GLOBAL_SCOPE} 表示无项目范围的通用卡；未配置 = 不赠送。
 */
public record StoredValueBonusPolicyRecord(
        String projectId,
        int bonusBps,
        int minBps,
        int maxBps,
        String updatedBy,
        Instant updatedAt
) {
    public static final String GLOBAL_SCOPE = "__global__";

    public static String scopeOf(String projectId) {
        return projectId == null || projectId.isBlank() ? GLOBAL_SCOPE : projectId;
    }
}
