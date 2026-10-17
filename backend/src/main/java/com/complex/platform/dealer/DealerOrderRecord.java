package com.complex.platform.dealer;

import java.time.Instant;

/**
 * 经销订单（ENUM:173 十五态：draft 草稿 / pending_review 待审核 / rejected 已驳回 / pending_payment 待付款 / payment_verifying 付款核验中 /
 * pending_shipment 待出库 / shipping 配送中 / signed 已签收 / completed 已完成 / cancelling 取消中 / cancelled 已取消 / pending_refund 待退款 /
 * returning 退货中 / partially_returned 部分退货 / returned 已退货）。通知付款时固化价格目录版本、销售主体、中心仓、运费模板版本（ENUM:180、BSM:1229）。
 */
public record DealerOrderRecord(
        String id,
        String orderNo,
        String dealerId,
        String dealerContractId,
        String salesEntityId,
        String centralWarehouseId,
        String status,
        String statusBeforeCancel,
        String priceCatalogId,
        boolean creditTerm,
        long goodsAmountCent,
        long freightCent,
        String freightBearer,
        int freightTemplateVersion,
        long totalAmountCent,
        long firstPaymentRequiredCent,
        Instant firstPaymentDeadlineAt,
        Instant balanceDueAt,
        long paidAmountCent,
        boolean overdue,
        String arApItemId,
        String reviewedBy,
        Instant reviewedAt,
        Instant paymentNotifiedAt,
        String shippedBy,
        Instant shippedAt,
        Instant signedAt,
        Instant completedAt,
        String cancelRequestedBy,
        String cancelReason,
        Instant cancelledAt,
        String note,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        /** CG-11c：累计已退（验收确认）金额，只由退货链回写，订单保存不覆盖。 */
        long returnedAmountCent
) {
    public DealerOrderRecord(String id, String orderNo, String dealerId, String dealerContractId, String salesEntityId, String centralWarehouseId, String status,
            String statusBeforeCancel, String priceCatalogId, boolean creditTerm, long goodsAmountCent, long freightCent, String freightBearer, int freightTemplateVersion,
            long totalAmountCent, long firstPaymentRequiredCent, Instant firstPaymentDeadlineAt, Instant balanceDueAt, long paidAmountCent, boolean overdue, String arApItemId,
            String reviewedBy, Instant reviewedAt, Instant paymentNotifiedAt, String shippedBy, Instant shippedAt, Instant signedAt, Instant completedAt,
            String cancelRequestedBy, String cancelReason, Instant cancelledAt, String note, String createdBy, Instant createdAt, Instant updatedAt) {
        this(id, orderNo, dealerId, dealerContractId, salesEntityId, centralWarehouseId, status, statusBeforeCancel, priceCatalogId, creditTerm, goodsAmountCent,
                freightCent, freightBearer, freightTemplateVersion, totalAmountCent, firstPaymentRequiredCent, firstPaymentDeadlineAt, balanceDueAt, paidAmountCent,
                overdue, arApItemId, reviewedBy, reviewedAt, paymentNotifiedAt, shippedBy, shippedAt, signedAt, completedAt, cancelRequestedBy, cancelReason,
                cancelledAt, note, createdBy, createdAt, updatedAt, 0L);
    }
}
