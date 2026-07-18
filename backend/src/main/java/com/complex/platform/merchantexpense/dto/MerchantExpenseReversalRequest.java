package com.complex.platform.merchantexpense.dto;

/** 红字冲销：对原台账行生成负金额新行（不改历史行）。note=冲销理由，必填。 */
public record MerchantExpenseReversalRequest(
        String idempotencyKey,
        String note) {
}
