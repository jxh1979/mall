package com.complex.platform.ticketing;

/**
 * P0 清单#2 / F-TXN-03 积分入账引擎事件 {@code points.confirm_requested}：核销 / 冲正后发布。
 *
 * <p>同步发布（与核销主链同事务）。customer 域 {@code LoyaltyPointsConsumer} 监听后调
 * {@code LoyaltyService.confirmPoints} 把该订单已确认积分份额对齐
 * {@code target = floor(总额 × verifiedUnits / totalUnits)}（按核销张数比例确认 / 冲正按份回滚）。
 * 幂等由 LoyaltyService 保证（对齐 target，重复事件 delta=0 noop）。
 *
 * <p>F-TXN-03：核销与冲正两路均发本事件并带最新张数计数——核销使 verifiedUnits 升、确认份额升；
 * 冲正使 verifiedUnits 降、确认份额按份回滚（取代原"冲正全额 deduct"路径）。
 *
 * @param orderId       出票订单 id（与 earn 同 source_doc=ticket_order/orderId 对齐）
 * @param verifiedUnits 该订单当前已核销张数（Σ used_times，核销后/冲正后的最新值）
 * @param totalUnits    该订单总张数（Σ total_times）；&lt;=0 退化为全额确认
 */
public record PointsConfirmRequestedEvent(
        String orderId,
        long verifiedUnits,
        long totalUnits
) {
}
