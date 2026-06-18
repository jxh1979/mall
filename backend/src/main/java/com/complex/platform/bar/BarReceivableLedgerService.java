package com.complex.platform.bar;

import java.util.List;

interface BarReceivableLedgerService {
    String createReceivableForOnAccountBill(
            BarBillRecord bill,
            BarOrderRecord order,
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds
    );

    /** A5(REQ-26b 增强)：卡座销售提成生成 finance 应付台账（source_type=employee_commission）。 */
    String createPayableForCommission(BarCommissionCalculationRecord commission);
}
