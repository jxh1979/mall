package com.complex.platform.ticketing;

/**
 * P0 清单#2 积分入账引擎事件 {@code points.confirm_requested}：核销/履约成功后发布。
 *
 * <p>同步发布（与核销主链同事务）。customer 域 {@code LoyaltyPointsConsumer} 监听后调
 * {@code LoyaltyService.confirmPoints} 把订单的 pending earn 转 confirmed（计入可用余额）。
 * 幂等由 LoyaltyService 保证（仅 pending 才确认，重复事件 noop）。
 *
 * @param orderId 出票订单 id（与 earn 同 source_doc=ticket_order/orderId 对齐）
 */
public record PointsConfirmRequestedEvent(
        String orderId
) {
}
