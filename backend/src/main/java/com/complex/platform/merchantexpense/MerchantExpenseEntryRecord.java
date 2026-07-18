package com.complex.platform.merchantexpense;

import java.time.Instant;

/**
 * 商户费用归集台账行（append-only）：分红净利润基数的费用侧数据。
 * source ∈ auto_energy（能耗应收挂接）/ auto_property（物业费账单挂接）/ manual（手工录入或红字冲销）。
 * 冲销行 = source='manual' 且 sourceRef=原台账行 id、amountCent 为原行取负。
 */
public record MerchantExpenseEntryRecord(
        String id,
        String merchantId,
        String period,
        String expenseType,
        long amountCent,
        String source,
        String sourceRef,
        String note,
        String operatorUserId,
        Instant createdAt) {

    /** 是否红字冲销行（manual 且携带原行引用）。 */
    public boolean isReversal() {
        return "manual".equals(source) && sourceRef != null;
    }
}
