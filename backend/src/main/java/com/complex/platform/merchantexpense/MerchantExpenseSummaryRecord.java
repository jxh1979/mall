package com.complex.platform.merchantexpense;

/** 期间汇总行：按商户 + 月份分类型费用合计（分红引擎费用侧取数点）。 */
public record MerchantExpenseSummaryRecord(
        String merchantId,
        String period,
        String expenseType,
        long totalCent) {
}
