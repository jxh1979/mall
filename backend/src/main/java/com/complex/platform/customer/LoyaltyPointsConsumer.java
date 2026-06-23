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
 *   <li>{@link PointsConfirmRequestedEvent}（核销 / 冲正）→ {@code confirmPoints} 把订单已确认份额对齐
 *       {@code target = floor(总额 × verifiedUnits / totalUnits)}（按核销张数比例确认 / 冲正按份回滚）；</li>
 *   <li>{@link PointsDeductRequestedEvent}（退款生效）→ {@code deductPoints} 按已确认份额比例冲减。</li>
 * </ul>
 *
 * <p>跨模块解耦：customer 已依赖 ticketing，ticketing 不可反向直接注入 LoyaltyService（循环依赖），
 * 故由本消费者（位于 customer 域）监听 ticketing 事件。幂等全部由 LoyaltyService 按 source_doc 保证。
 *
 * <p>F-TXN-03 按核销张数：核销与冲正两路均发 {@link PointsConfirmRequestedEvent} 并带最新 Σused/Σtotal 计数——
 * 核销使已确认份额按张数升、冲正使其按份降（取代原"冲正发独立事件全额 deduct"的 100% 回滚）。
 * 退款 deduct 仍以 {@code (ticket_refund, refundId)} 为独立幂等键、按已确认份额比例冲减
 * （现行"核销后不可退"使退款只发生在全未核销 → 已确认份额=0 → 自然 noop）。
 */
@Component
public class LoyaltyPointsConsumer {

    /** 出票订单单据类型（earn/confirm 的 source_doc_type）。 */
    static final String DOC_TYPE_TICKET_ORDER = "ticket_order";
    /** 退款单据类型（退款 deduct 的幂等键 source_doc_type）。 */
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
        // F-TXN-03：把该订单已确认积分份额对齐 floor(总额 × 已核销张数 / 订单总张数)；
        // 无会员订单从未入账 / 对齐 target 无变化（含重复事件）时 LoyaltyService 自身幂等 noop。
        loyaltyService.confirmPoints(
                DOC_TYPE_TICKET_ORDER, event.orderId(), event.verifiedUnits(), event.totalUnits());
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
