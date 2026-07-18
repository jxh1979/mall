package com.complex.platform.merchantexpense.dto;

/**
 * 手工录入商户费用（人工/摊销/其他等）。amountCent 为分、不可为 0；note=录入理由，必填（审计留痕）。
 */
public record MerchantExpenseEntryRequest(
        String idempotencyKey,
        String merchantId,
        String period,
        String expenseType,
        Long amountCent,
        String note) {
}
