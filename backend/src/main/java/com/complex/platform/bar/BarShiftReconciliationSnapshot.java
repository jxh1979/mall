package com.complex.platform.bar;

/**
 * 班结系统应收聚合快照（V095）。按班次时间窗口聚合 bar_bill_settlements 各支付方式、退款、小费。
 *
 * <p>口径：现金/POS/线上为已结账单结算流水（settled_at 落窗口）按 settle_channel 归并；退款为
 * bar_bill_adjustments(type=refund) 按 refunded_at 落窗口；小费为窗口内结算账单的 tip_amount_cent。
 * 现金/POS 另含本收银员登记（deposit_collector_staff_id）且 deposit_paid_at 落窗口的订台定金（第十八批 O 段 126-R2）。
 * 只读聚合，不改账单、不二次过账。
 */
public record BarShiftReconciliationSnapshot(
        long systemCashCent,
        long systemPosCent,
        long systemOnlineCent,
        long systemRefundCent,
        long systemTipCent
) {
    static BarShiftReconciliationSnapshot empty() {
        return new BarShiftReconciliationSnapshot(0L, 0L, 0L, 0L, 0L);
    }
}
