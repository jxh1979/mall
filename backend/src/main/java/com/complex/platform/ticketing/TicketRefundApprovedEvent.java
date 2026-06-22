package com.complex.platform.ticketing;

/**
 * 票务退款审批通过（生效）事件 {@code ticket.refund_approved}：退款打款/拨付的解耦信号。
 *
 * <p>同步发布（与退款审批主链同事务，与积分冲减事件一致）。供财务/拨付侧据此发起实际打款
 * （原渠道退回/现金/第三方），并按 refundChannel 选择路径。幂等由消费侧按 refundId 保证。
 * 退款金额本身已落 ticket_refunds（业务表），看板 net 按业务表口径扣减（C1），故此事件只承载打款拨付，
 * 不重复入账。
 *
 * @param orderId        原出票订单 id
 * @param refundId       退款单 id（打款幂等键来源）
 * @param refundAmountCent 退款金额（分）
 * @param refundChannel  退款渠道（original 原路退回 / cash / third_party 等）
 */
public record TicketRefundApprovedEvent(
        String orderId,
        String refundId,
        long refundAmountCent,
        String refundChannel
) {
}
