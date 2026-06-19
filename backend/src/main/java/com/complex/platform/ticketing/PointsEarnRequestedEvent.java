package com.complex.platform.ticketing;

/**
 * P0 清单#2 积分入账引擎事件 {@code points.earn_requested}：出票订单支付成功（pay_status→paid）后发布。
 *
 * <p>事件驱动、同步发布（与出票主链同事务，便于测试且保证留痕一致）。customer 域
 * {@code LoyaltyPointsConsumer} 监听后调 {@code LoyaltyService.earnPointsPending} 写 pending 入账。
 * 跨模块解耦：ticketing 不能直接依赖 customer（customer 已依赖 ticketing，直接注入会形成循环依赖），
 * 故沿用 ticketing→bar 既有 {@code GiftVerifiedEvent} 的事件解耦模式。
 *
 * <p>customerId 可空（如 admin/kiosk 渠道无会员）；消费者侧 customerId 为空即跳过（不入账）。
 *
 * @param orderId      出票订单 id（幂等键来源：source_doc=ticket_order/orderId）
 * @param customerId   会员 id（可空：无会员订单不入积分）
 * @param basePoints   基础积分（口径：订单实付金额对应的基础分，倍率由 LoyaltyService 叠加）
 * @param validityDays 积分有效期天数（可空：不过期）
 */
public record PointsEarnRequestedEvent(
        String orderId,
        String customerId,
        long basePoints,
        Integer validityDays
) {
}
