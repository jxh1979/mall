package com.complex.platform.finance;

// 文件版本：V0.01

/**
 * 退租结算收付回写端口（合同域实现，finance 经 @Lazy 反向依赖调用，避免初始化环；与 {@link TerminationSettlementPort} 反向同范式）。
 * 2026-10-18 第三轮裁决第 4 条「押金退还付清后→「已结清」」「差额收齐后→「已结清」」：台账项每登记一笔收付款
 * （FinanceService.settleArApItem 统一回写点）、补录调整审批通过（红冲＋重述改了未清额，入参＝重述行）即在同一事务内回调，
 * 由合同域推进「押金抵扣中」「欠费追缴中」的退租结算单。
 */
public interface TerminationSettlementProgressPort {

    /** item＝已回写 paid/unpaid/status 的台账项。 */
    void onArApItemPaid(ArApItemRecord item);
}
