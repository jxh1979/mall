package com.complex.platform.integration;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 批6 第三方通票回流 → 本地出票端口（integration → ticketing 单向依赖的窄接口）。
 *
 * <p>实现方（{@link TicketingChannelTicketIssueAdapter}）走既有出票主链：
 * 下单（sales_channel=具体渠道，结算 S11 渠道费据此按渠道解析）→ 支付意图 → 支付确认 → 出票
 * （权益拆分/票码防伪/分摊快照/S3 父票/S11 分项价全部复用，不另造出票路径）。
 * 全链幂等键由 idempotencyBase 派生（同一渠道订单号重复回流只出一次票）。
 */
public interface ChannelTicketIssuePort {

    IssueResult issue(IssueCommand command);

    /** 自动对账系统取数：按本地票订单 id 取实收金额（平台侧期望值，直读 ticket_orders 真源）。 */
    Optional<Long> findOrderAmountCent(String ticketOrderId);

    record IssueCommand(
            String idempotencyBase,
            String ticketTypeId,
            String priceRuleId,
            int quantity,
            LocalDate visitDate,
            String salesChannel,
            String buyerPhone,
            String reason
    ) {
    }

    record IssueResult(
            String ticketOrderId,
            String orderNo,
            long amountCent,
            int issuedTicketCount,
            int issuedEntitlementCount
    ) {
    }
}
