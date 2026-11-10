package com.complex.platform.bar;

// 文件版本：V0.04

import java.time.LocalDate;
import java.util.List;

interface BarReceivableLedgerService {

    /**
     * R5 T7 协同：经营日是否被锁定日结纳入；反结改动结账后金额前校验，锁定则须走补录审批。
     * 第十八批 Z 段第 187 条 187-R1（18R 143-R7、18S 161-R1 按日结 scope_id 判锁）：按账单所属酒吧商户判，不按操作人范围。
     */
    boolean isBusinessDayLocked(LocalDate businessDate, String merchantId);

    String createReceivableForOnAccountBill(
            BarBillRecord bill,
            BarOrderRecord order,
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds
    );

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

    /**
     * F-TXN-03 piece③b 通票免费酒水平台↔酒吧结算：核销发放免费酒水后，平台按该酒水内部结算价向酒吧(merchant)挂应付
     * （direction='payable'、source_type='pass_drink_settlement'、source_id/幂等键=verificationId）。返回 ar_ap 项 id。
     */
    /**
     * TK-20-R2 退品驳回：把该退款调整已回冲到储值卡的部分重新扣回（与 createRefundPayable 的储值回卡对称）。
     * default no-op 供内存测试双子兼容；返回扣回金额（分）。
     */
    default long reverseStoredValueRefund(BarBillRecord bill, BarOrderRecord order, String originalAdjustmentId,
                                          String reversalIdempotencyKey, String reason) {
        return 0L;
    }

    /**
     * 第十八批 AA 段第 202 条 202-R1（第十八批裁决 39「反结生效时储值部分当场回原卡；现金、POS 等收款记录作废，收银按更正后的账单重新收款」）：
     * 反结时把该账单经储值支付、尚未回冲的部分当场退回原卡；其余渠道不挂退款应付。default no-op 供内存测试双子兼容。
     */
    default void refundStoredValueOnReopen(BarBillRecord bill) {
    }

    /**
     * 第十批 TK-24-R3：订台定金达标原路退回——过一条 direction='payable'、source_type='bar_deposit_refund'、
     * source_id=reservation.id 的应付退款项（V008 唯一索引天然幂等），实付由既有 finance 收付款登记完成。
     * default null 供内存测试双子兼容。
     */
    default String createDepositRefundPayable(BarReservationRecord reservation) {
        return null;
    }

    /** 复核 money I2：该订台的定金原路退款应付是否已挂出（退款处理中）；default false 供内存测试双子兼容。 */
    default boolean hasDepositRefundPayable(String reservationId) {
        return false;
    }

    String createPayableForGiftSettlement(String verificationId, String merchantId, long internalPriceCent);

    /**
     * 第十八批 R 段第 142 条 142-R1（BSM:1081「长款当期计入该商户营业外收入并单独留痕可查」）：复核平账的收银长款记该酒吧内部核算台账
     * （source_type='bar_shift_overage'、source_id＝班次）。default no-op 供内存测试双子兼容。
     */
    default void postShiftOverageIncome(String shiftId, String barId, long amountCent) {
    }

    /**
     * 赠饮核销冲正 → 冲销该结算应付：过一条 direction='receivable'、source_type='pass_drink_settlement'、
     * 同 source_id=verificationId（direction 不同 → 与原应付净额归零）。返回 ar_ap 项 id。
     */
    String reverseGiftSettlementPayable(String verificationId, String merchantId, long internalPriceCent);
}
