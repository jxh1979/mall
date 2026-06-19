package com.complex.platform.bar;

import java.time.LocalDate;
import java.util.List;

interface BarReceivableLedgerService {

    /** R5 T7 协同：经营日是否被锁定日结纳入；反结改动结账后金额前校验，锁定则须走补录审批。 */
    boolean isBusinessDayLocked(LocalDate businessDate);

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

    /**
     * 延期项④：退菜退酒审批驳回时反向冲销已过账的退款应付。
     * 原退款过账为 direction='payable'、source_type='refund' 的"应退/已退"项；驳回后该退款无效，
     * 故过一条 direction='receivable'、source_type='refund' 的对冲项（"应退转应收/收回"语义），
     * source_id=驳回冲销调整记录 id（与原退款项 source_id 不同），与 V008 唯一索引
     * (direction, source_type, source_id) 对齐保持幂等，使财务台账退款净额归零。
     */
    String reverseRefundPayable(
            BarBillRecord bill,
            BarOrderRecord order,
            String reversalAdjustmentId,
            long refundAmountCent,
            String idempotencyKey,
            String reason,
            List<String> evidenceAttachmentIds
    );
}
