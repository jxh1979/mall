package com.complex.platform.dealer;

import java.time.Instant;

/** 经销电子发票。status 沿用 ENUM「开票状态」：pending / issuing / issued / reversed / voided / failed；开票主体＝合同销售主体（ENUM:182）。 */
public record DealerInvoiceRecord(
        String id,
        String orderId,
        String dealerId,
        String salesEntityId,
        long amountCent,
        Integer taxRateBps,
        String status,
        String invoiceNo,
        String reversedByReturnId,
        String reissuedFromInvoiceId,
        String requestedBy,
        Instant requestedAt,
        String issuedBy,
        Instant issuedAt,
        Instant reversedAt,
        String note
) {
}
