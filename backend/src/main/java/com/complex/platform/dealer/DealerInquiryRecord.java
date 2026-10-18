package com.complex.platform.dealer;

import java.time.Instant;
import java.time.LocalDate;

/** 经销询价。status：pending_quote 待报价 / quote_approval 报价审批中 / quoted 已报价 / expired 已失效（ENUM:171）。 */
public record DealerInquiryRecord(
        String id,
        String dealerId,
        String dealerContractId,
        String itemId,
        String spec,
        int quantity,
        String status,
        Long quotedPriceCent,
        String quotedBy,
        Instant quotedAt,
        LocalDate quoteValidUntil,
        String approvedBy,
        Instant approvedAt,
        String note,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
