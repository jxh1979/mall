package com.complex.platform.dividend.dto;

/**
 * 期间分红作业触发：kind=official（正式单，periodLabel 须匹配方案周期类型：季度方案=2026-Q3 / 月度方案=2026-07）
 * 或 preview（月度预览单，periodLabel=yyyy-MM，仅季度方案出预览）。merchantId 为空=对全部生效中方案跑批。
 * 幂等可重跑：draft/preview/rejected 单作废重算；已进入审批流（pending_review/approved/paid）跳过不动。
 */
public record DividendComputeRequest(
        String idempotencyKey,
        String periodLabel,
        String kind,
        String merchantId) {
}
