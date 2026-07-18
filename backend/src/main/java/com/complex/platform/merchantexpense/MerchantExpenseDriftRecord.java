package com.complex.platform.merchantexpense;

/**
 * 自动挂接金额漂移行：某个已挂接上游单据的台账净额（挂接原行 + 指向它的红字冲销行合计）
 * 与上游单据当前金额不一致。处置 SOP：红字冲销台账旧行，再手工录入修正额（台账仅追加，不回改）。
 */
public record MerchantExpenseDriftRecord(
        String entryId,
        String merchantId,
        String source,
        String sourceRef,
        long ledgerNetCent,
        long upstreamAmountCent) {
}
