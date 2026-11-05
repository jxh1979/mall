package com.complex.platform.inventory;

// 文件版本：V0.01

/**
 * 第十八批 T 段第 157 条 157-R4：高价值挂起订单转坏账复核（subject_type {@code bar_order_bad_debt}，subject_id＝酒吧订单）的生效端口。
 * 依据：BSM:1112「达到既有高价值标准的商品仍执行原证据和审批要求。」；BSM:1109「在转入该状态前继续阻断」。
 *
 * <p>依赖方向 inventory → 本端口 ← bar（实现方 {@code BarOrderSuspensionService}）：库存域内高价值复核批准时由酒吧域把挂起转坏账损耗处理中，
 * 驳回时撤回待复核标记；库存域不感知挂起表结构——与 {@code BarCommissionRateConfigPort} 同一套端口范式。在复核事务内调用。
 */
public interface BarOrderBadDebtReviewPort {

    /** 复核批准：以发起经理为确认人转坏账损耗处理中，复核人记审计。 */
    void approved(String orderId, String reviewId, String reviewerUserId);

    /** 复核驳回：挂起保持已挂起待付款，清空待复核的复核单，可重新发起。 */
    void rejected(String orderId, String reviewId, String reviewerUserId);
}
