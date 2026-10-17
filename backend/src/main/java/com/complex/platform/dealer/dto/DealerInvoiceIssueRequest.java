package com.complex.platform.dealer.dto;

/** 开具（第三方开票平台回填发票号）/ 红冲后重开时的新发票号（可空＝重开发票待开具）。 */
public record DealerInvoiceIssueRequest(String idempotencyKey, String invoiceNo) {
}
