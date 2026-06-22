package com.complex.platform.finance;

import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ticketing.TicketRefundApprovedEvent;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 票务退款打款拨付消费者（事件驱动，同步 {@link EventListener}，与退款审批主链同事务）。
 *
 * <p>监听 ticketing 域 {@link TicketRefundApprovedEvent}（退款审批生效）→ 留痕一条"退款拨付待执行"审计事件，
 * 使打款拨付信号可追溯、可对账，不再悬空丢失（修审计 P2：事件无消费者）。实际打款由财务/支付侧据 refund_channel
 * 发起（原路退回/现金/第三方），与 confirmOffline 入站确认对称；退款金额本身已落 ticket_refunds（业务表，net 按 C1 扣减），
 * 本消费者只承载拨付指令留痕，不重复入账。幂等由审计事件按 refundId 保证。
 */
@Component
public class TicketRefundPayoutConsumer {

    private final AuditEventService auditEventService;

    public TicketRefundPayoutConsumer(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @EventListener
    public void onTicketRefundApproved(TicketRefundApprovedEvent event) {
        if (event == null || event.refundId() == null || event.refundId().isBlank()) {
            return;
        }
        auditEventService.recordSuccess(
                "finance.ticket_refund.payout_pending",
                "finance.payment.register",
                "ticket_refund",
                event.refundId(),
                null,
                event.refundId(),
                Map.of(
                        "order_id", event.orderId() == null ? "" : event.orderId(),
                        "refund_amount_cent", event.refundAmountCent(),
                        "refund_channel", event.refundChannel() == null ? "original" : event.refundChannel()
                )
        );
    }
}
