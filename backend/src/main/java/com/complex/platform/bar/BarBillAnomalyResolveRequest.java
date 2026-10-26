package com.complex.platform.bar;

// 文件版本：V0.01

/**
 * 第十六批第 16 条（ENUM「异常状态的统一出口」）：主管处置异常账单。resolution 取 restored（更正后恢复）/ voided_reversed（作废并冲正），
 * reason 必填留痕。
 */
public record BarBillAnomalyResolveRequest(
        String idempotencyKey,
        String resolution,
        String reason
) {
}
