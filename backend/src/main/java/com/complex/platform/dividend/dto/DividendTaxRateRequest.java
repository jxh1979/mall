package com.complex.platform.dividend.dto;

// 文件版本：V0.01

import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import java.time.LocalDate;
import java.util.List;

/**
 * 第十八批 AX 段第 271 条 271-R1 提交全平台统一分红代扣个税税率新版本（BSM:1776「安全阈值，全平台统一可配，调整须经财务、税务、法务三审并由顶层平台公司
 * 最终确认，出厂默认 20%，须同时配置上下限」）。rateBps、minBps、maxBps 为万分比，下限 ≤ 税率 ≤ 上限；effectiveFrom 生效起始日期与 reason 理由必填；
 * parallelLines 为财务、税务、法务三条线的最终审批人。
 */
public record DividendTaxRateRequest(
        String idempotencyKey,
        Integer rateBps,
        Integer minBps,
        Integer maxBps,
        LocalDate effectiveFrom,
        String reason,
        List<SupervisorReviewEvaluateRequest.ParallelLine> parallelLines) {
}
