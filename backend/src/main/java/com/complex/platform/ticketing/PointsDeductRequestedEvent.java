package com.complex.platform.ticketing;

/**
 * P0 清单#2 积分入账引擎事件 {@code points.deduct_requested}：退款审批通过（生效）后发布。
 *
 * <p>同步发布（与退款审批主链同事务）。customer 域 {@code LoyaltyPointsConsumer} 监听后调
 * {@code LoyaltyService.deductPoints} 按退款比例冲减原单已 confirmed 的积分（只冲已入账部分）。
 * 幂等由 LoyaltyService 按 (ticket_refund, refundId, deduct) 保证。
 *
 * @param orderId       原出票订单 id（earn 落账的 source_doc）
 * @param refundId      退款单 id（deduct 幂等键来源：source_doc=ticket_refund/refundId）
 * @param refundRatioBp 退款比例（basis points，退款额/原单额×10000；按已 confirmed 积分等比例冲减）
 */
public record PointsDeductRequestedEvent(
        String orderId,
        String refundId,
        long refundRatioBp
) {
}
