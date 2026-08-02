package com.complex.platform.integration;

import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.TicketOrderRecord;
import com.complex.platform.ticketing.TicketPaymentIntentRecord;
import com.complex.platform.ticketing.IssuedTicketResponse;
import com.complex.platform.ticketing.TicketingService;
import com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 批6 {@link ChannelTicketIssuePort} 实现：第三方通票回流走既有出票主链（不另造出票路径）。
 *
 * <p>四步全链（每步各自幂等，键由 idempotencyBase 派生，重复回流全链重放同一结果）：
 * <ol>
 *   <li>下单：sales_channel=具体渠道（meituan/douyin…）→ 结算 S11 渠道费按 ticket_orders.sales_channel
 *       解析该渠道费率（channel_fee_configs 渠道专属&gt;通用，V327/V381 比例/固定额两模式）；</li>
 *   <li>支付意图：渠道即支付通道（客人已在第三方付讫，本地登记该渠道收款）；</li>
 *   <li>支付确认：paid_at=确认时刻（强制口径:下单+支付时间都记）；</li>
 *   <li>出票：权益拆分/票码防伪（HMAC 动态码）/收入分摊快照/S3 父票实例/S11 分项价校验全部复用。</li>
 * </ol>
 * 渠道客人非本地会员（customerId=null）：积分入账事件由 customer 域按无会员单跳过，语义正确。
 */
@Component
public class TicketingChannelTicketIssueAdapter implements ChannelTicketIssuePort {

    private final TicketingService ticketingService;

    public TicketingChannelTicketIssueAdapter(TicketingService ticketingService) {
        this.ticketingService = ticketingService;
    }

    @Override
    public IssueResult issue(IssueCommand command) {
        TicketOrderRecord order = ticketingService.createTicketOrder(new TicketOrderRequest(
                command.idempotencyBase() + "_order",
                command.reason(),
                List.of(),
                command.ticketTypeId(),
                command.priceRuleId(),
                command.quantity(),
                command.visitDate(),
                command.salesChannel(),
                command.buyerPhone(),
                null,
                Map.of()
        ));
        TicketPaymentIntentRecord intent = ticketingService.createTicketOrderPayIntent(order.id(),
                new PaymentIntentRequest(
                        command.idempotencyBase() + "_pay",
                        command.reason(),
                        List.of(),
                        "ticket_order",
                        order.id(),
                        order.amountCent(),
                        "CNY",
                        command.salesChannel()
                ));
        ticketingService.confirmTicketOrderPayment(order.id(),
                new ConfirmTicketOrderPaymentRequest(command.idempotencyBase() + "_confirm", intent.id(), null));
        IssuedTicketResponse issued = ticketingService.issueTicketOrder(order.id(),
                new StatusChangeRequest(command.idempotencyBase() + "_issue", null, command.reason(), List.of()));
        return new IssueResult(
                order.id(),
                order.orderNo(),
                order.amountCent(),
                // 按张出票（AUD-066）：一单 quantity 张 = quantity 个父票实例。
                order.quantity(),
                issued.issuedEntitlements().size()
        );
    }

    @Override
    public Optional<Long> findOrderAmountCent(String ticketOrderId) {
        return ticketingService.findTicketOrderById(ticketOrderId).map(TicketOrderRecord::amountCent);
    }
}
