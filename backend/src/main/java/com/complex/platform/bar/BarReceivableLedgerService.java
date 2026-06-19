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

    /**
     * RC1①：酒吧结账后退款（adjustment_type='refund'）过账到 finance ar_ap 台账。
     * 生成一条 direction='payable'、source_type='refund' 的应付项（"应退/已退"语义），
     * source_id=退款调整记录 id，counterparty=客户/账单引用，amount=退款额。
     */
    String createRefundPayable(
            BarBillRecord bill,
            BarOrderRecord order,
            String adjustmentId,
            long refundAmountCent,
            String idempotencyKey,
            String reason,
            List<String> evidenceAttachmentIds
    );
}
