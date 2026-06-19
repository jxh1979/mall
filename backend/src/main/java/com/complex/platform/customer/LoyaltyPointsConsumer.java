package com.complex.platform.customer;

import com.complex.platform.ticketing.PointsConfirmRequestedEvent;
import com.complex.platform.ticketing.PointsDeductRequestedEvent;
import com.complex.platform.ticketing.PointsEarnRequestedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * P0 清单#2 积分入账消费者（事件驱动，同步 {@link EventListener}，与触发主链同事务）。
 *
 * <p>监听 ticketing 域积分事件并调 {@link LoyaltyService} 完成入账：
 * <ul>
 *   <li>{@link PointsEarnRequestedEvent}（支付成功）→ {@code earnPointsPending} 写 pending；</li>
 *   <li>{@link PointsConfirmRequestedEvent}（核销/履约）→ {@code confirmPoints} 转 confirmed；</li>
 *   <li>{@link PointsDeductRequestedEvent}（退款生效）→ {@code deductPoints} 按比例冲减已入账。</li>
 * </ul>
 *
 * <p>跨模块解耦：customer 已依赖 ticketing，ticketing 不可反向直接注入 LoyaltyService（循环依赖），
 * 故由本消费者（位于 customer 域）监听 ticketing 事件。幂等全部由 LoyaltyService 按 source_doc 保证。
 *
 * <p>单据类型常量：earn/confirm 用 {@code ticket_order}，deduct 用 {@code ticket_refund}。
 */
@Component
public class LoyaltyPointsConsumer {

    /** 出票订单单据类型（earn/confirm 的 source_doc_type）。 */
    static final String DOC_TYPE_TICKET_ORDER = "ticket_order";
    /** 退款单据类型（deduct 的幂等键 source_doc_type）。 */
    static final String DOC_TYPE_TICKET_REFUND = "ticket_refund";

    private final LoyaltyService loyaltyService;

    public LoyaltyPointsConsumer(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @EventListener
    public void onPointsEarnRequested(PointsEarnRequestedEvent event) {
        // 无会员（admin/kiosk 等渠道）或无积分：跳过，不入账。
        if (event.customerId() == null || event.customerId().isBlank() || event.basePoints() <= 0) {
            return;
        }
        loyaltyService.earnPointsPending(
                event.customerId(),
                event.basePoints(),
                DOC_TYPE_TICKET_ORDER,
                event.orderId(),
                "ticket order " + event.orderId(),
                event.validityDays());
    }

    @EventListener
    public void onPointsConfirmRequested(PointsConfirmRequestedEvent event) {
        if (event.orderId() == null || event.orderId().isBlank()) {
            return;
        }
        // 幂等 noop：无 pending（无会员订单从未入账 / 已确认）则什么都不做。
        loyaltyService.confirmPoints(DOC_TYPE_TICKET_ORDER, event.orderId());
    }

    @EventListener
    public void onPointsDeductRequested(PointsDeductRequestedEvent event) {
        if (event.orderId() == null || event.orderId().isBlank()
                || event.refundId() == null || event.refundId().isBlank()
                || event.refundRatioBp() <= 0) {
            return;
        }
        loyaltyService.deductPoints(
                DOC_TYPE_TICKET_ORDER,
                event.orderId(),
                DOC_TYPE_TICKET_REFUND,
                event.refundId(),
                event.refundRatioBp());
    }
}
