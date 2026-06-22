package com.complex.platform.customer;

import com.complex.platform.ticketing.PointsConfirmRequestedEvent;
import com.complex.platform.ticketing.PointsDeductRequestedEvent;
import com.complex.platform.ticketing.PointsEarnRequestedEvent;
import com.complex.platform.ticketing.VerificationReversedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * P0 清单#2 积分入账消费者（事件驱动，同步 {@link EventListener}，与触发主链同事务）。
 *
 * <p>监听 ticketing 域积分事件并调 {@link LoyaltyService} 完成入账：
 * <ul>
 *   <li>{@link PointsEarnRequestedEvent}（支付成功）→ {@code earnPointsPending} 写 pending；</li>
 *   <li>{@link PointsConfirmRequestedEvent}（核销/履约）→ {@code confirmPoints} 转 confirmed；</li>
 *   <li>{@link PointsDeductRequestedEvent}（退款生效）→ {@code deductPoints} 按比例冲减已入账；</li>
 *   <li>{@link VerificationReversedEvent}（核销冲正/撤销履约）→ {@code deductPoints} 全额冲减该订单已 confirmed 积分。</li>
 * </ul>
 *
 * <p>跨模块解耦：customer 已依赖 ticketing，ticketing 不可反向直接注入 LoyaltyService（循环依赖），
 * 故由本消费者（位于 customer 域）监听 ticketing 事件。幂等全部由 LoyaltyService 按 source_doc 保证。
 *
 * <p>单据类型常量：earn/confirm 用 {@code ticket_order}，退款 deduct 用 {@code ticket_refund}，
 * 核销冲正 deduct 用 {@code ticket_verification_reversal}（冲正专用幂等键，与退款 deduct 互不冲突）。
 *
 * <p>F-TXN-03（裁决#3）核销冲正回滚积分：核销成功发 {@link PointsConfirmRequestedEvent} 把订单积分 pending→confirmed；
 * 与之对称，核销冲正（撤销履约）发 {@link VerificationReversedEvent}，本消费者据此对该订单已 confirmed 积分做
 * 全额冲减（撤销履约即撤销其确认入账）。复用 {@link LoyaltyService#deductPoints} 的"只冲已 confirmed、不冲 pending、
 * 账户行锁防超兑、余额下限 0"既有口径；以"核销冲正"专用单据 {@code (ticket_verification_reversal, verificationId)}
 * 作幂等键，区别于退款 deduct 的 {@code (ticket_refund, refundId)}，避免重复/冲突。
 */
@Component
public class LoyaltyPointsConsumer {

    /** 出票订单单据类型（earn/confirm 的 source_doc_type）。 */
    static final String DOC_TYPE_TICKET_ORDER = "ticket_order";
    /** 退款单据类型（退款 deduct 的幂等键 source_doc_type）。 */
    static final String DOC_TYPE_TICKET_REFUND = "ticket_refund";
    /** 核销冲正单据类型（冲正 deduct 的幂等键 source_doc_type，专用以区别退款 deduct）。 */
    static final String DOC_TYPE_VERIFICATION_REVERSAL = "ticket_verification_reversal";
    /** 核销冲正=撤销履约，按全额（100% = 10000 basis points）冲减该订单已 confirmed 积分。 */
    static final long FULL_REVERSAL_RATIO_BP = 10_000L;

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

    /**
     * F-TXN-03（裁决#3）：核销冲正（撤销履约）→ 全额冲减该订单已 confirmed 积分（与"核销→confirm"对称）。
     *
     * <p>仅处理有订单（orderId 非空）的核销：直接发放无订单场景本就不入积分，无可冲减。
     * 以核销冲正专用单据 {@code (ticket_verification_reversal, verificationId)} 为幂等键——
     * 同一核销重复冲正事件只冲减一次；且与退款 deduct 的 {@code (ticket_refund, refundId)} 不同键，
     * 退款链路不受影响（各自独立幂等行）。
     *
     * <p>冲减口径完全复用 {@link LoyaltyService#deductPoints}：只冲已 confirmed（pending 不冲）、账户行锁防并发超兑、
     * 余额不低于 0。原订单未履约（积分仍 pending）或无会员（从未入账）时 deductPoints 自身 noop 返回 0。
     */
    @EventListener
    public void onVerificationReversed(VerificationReversedEvent event) {
        if (event == null) {
            return;
        }
        if (event.orderId() == null || event.orderId().isBlank()
                || event.verificationId() == null || event.verificationId().isBlank()) {
            return; // 无订单：无积分可冲减；无核销 id：无法构成冲正专用幂等键。
        }
        loyaltyService.deductPoints(
                DOC_TYPE_TICKET_ORDER,
                event.orderId(),
                DOC_TYPE_VERIFICATION_REVERSAL,
                event.verificationId(),
                FULL_REVERSAL_RATIO_BP);
    }
}
