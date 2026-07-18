package com.complex.platform.merchantexpense;

/** 自动挂接执行结果：本次新计入的能耗 / 物业费行数（重跑幂等，已计入的为 0）。 */
public record MerchantExpenseSyncResultRecord(
        String period,
        int energyInserted,
        int propertyInserted) {
}
