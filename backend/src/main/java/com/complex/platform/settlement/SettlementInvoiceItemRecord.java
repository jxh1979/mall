package com.complex.platform.settlement;

import java.time.LocalDate;

/** 批 4b：开票清单条目（settlement_invoice_items 一行）。金额均整数分。 */
public record SettlementInvoiceItemRecord(
        String id,
        String billId,
        /** 池分账行来源；null=结算单头级（如门票综合服务费）。 */
        String settlementItemId,
        String projectId,
        LocalDate periodStart,
        LocalDate periodEnd,
        String incomeType,
        /** 开票主体 business_entities.id；归属未配置时为 null（清单仍出、页面提示去配）。 */
        String subjectEntityId,
        String subjectName,
        String invoiceItemName,
        String taxCategory,
        String evidenceRequired,
        long amountCent,
        int vatRateBps,
        long vatAmountCent,
        boolean culturalFeeApplicable,
        int culturalFeeRateBps,
        long culturalFeeCent,
        String status,
        String remark,
        String updatedBy
) {
}
