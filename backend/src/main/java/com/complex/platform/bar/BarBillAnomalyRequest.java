package com.complex.platform.bar;

// 文件版本：V0.01

/** 第十六批第 16 条：客人对账单提出争议时员工登记置异常（reason＝争议原因，必填留痕）。 */
public record BarBillAnomalyRequest(
        String idempotencyKey,
        String reason
) {
}
