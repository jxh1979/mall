package com.complex.platform.merchantexpense.dto;

/** 自动挂接触发：把 period（如 2026-07）已出账的能耗费应收与物业费账单汇入费用台账，幂等可重跑。 */
public record MerchantExpenseSyncRequest(
        String idempotencyKey,
        String period) {
}
