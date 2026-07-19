package com.complex.platform.finance;

import java.util.List;

/**
 * 持股分红发放端口（V299）：分红计算单审批通过后，按主理人净发放额（代扣后）挂应付
 * （ar_ap_items direction=payable, source_type='dividend_payout', counterparty=主理人账户,
 * source_id=计算单 id——(direction,source_type,source_id) 部分唯一索引天然防一单重挂）。
 * finance 侧实现；依赖方向 dividend→finance。返回 ar_ap_item_id。
 *
 * <p>V303 欠款抵扣：计算时读该商户未清应收快照（{@link #listOutstandingReceivables}），审批时按冻结抵扣额
 * 对该商户应收逐项结算（{@link #applyDividendOffsetToReceivables}，FOR UPDATE 行锁+dividend_offset 收款留痕）。
 */
public interface DividendPayoutPort {

    String createDividendPayoutPayable(String principalUserId, String merchantId, long netAmountCent, String statementId);

    /**
     * V303 发放保护线留存：审批时把留存额记为「欠主理人的待释放款」——挂应付
     * (payable, counterparty=主理人, source_type='dividend_reserve_withheld', source_id=计算单 id)，非平台白拿。
     * 释放（下期/年度末返还等规则 + 手动释放端点）为后续独立构建 R，本端口只落待释放负债。返回 ar_ap_item_id。
     */
    String createDividendReserveWithholdingPayable(
            String principalUserId, String merchantId, long reserveCent, String statementId);

    /** 未清应收快照（direction=receivable，unpaid>0，按 created_at 升序=先欠先抵），供计算单冻结欠款抵扣额与逐项留痕。 */
    List<DividendArrearsItem> listOutstandingReceivables(String merchantId);

    /**
     * 审批时按冻结抵扣额对该商户未清应收逐项结算（先欠先抵，dividend_offset 收款）。
     * 返回实际结算总额；调用方断言 == 冻结抵扣额，不等（抵扣期间应收被并发清偿）由 @Transactional 回滚并要求重算。
     */
    long applyDividendOffsetToReceivables(String merchantId, long offsetCent, String statementId);

    /** 未清应收明细（供逐项留痕）：台账项 id / 单号 / 未清额（分）。 */
    record DividendArrearsItem(String arApItemId, String itemNo, long unpaidCent) {
    }
}
