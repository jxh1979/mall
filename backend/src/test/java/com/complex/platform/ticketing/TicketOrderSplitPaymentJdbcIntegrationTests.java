package com.complex.platform.ticketing;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketRefundRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03-R20 组合支付（操作员原话「一个订单可分多个渠道支付，但最终必须完成一次支付才算完成」；BSM 第 1749 行「混合支付的退款按收款相反顺序
 * 逐笔原路退回（后收的先退，单笔退足再退上一笔）；储值部分回冲仍按先赠送金后本金的既有顺序」）：储值先付一部分、在线补足合计足额才完成并出票；
 * 未足额超时取消的已收各笔原路退回；退款从后收的一笔退起。客人端请求按真实形状（蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TicketOrderSplitPaymentJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private TicketingService ticketingService;
    @Autowired private TicketRefundService refundService;
    @MockitoBean private PaymentGatewayRouter gateway;

    private CustomerTicketPaymentFixture f;

    @BeforeEach
    void seed() {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 20_000L);
        f.stubGateway(gateway);
    }

    @AfterEach
    void clear() {
        TestSecurityActors.clear();
    }

    @Test
    void storedValuePartialLeavesOrderUnpaidAndUnissued() throws Exception {
        String orderId = f.createOrderId(2);
        f.payWithStoredValue(orderId, 3000L).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pay_status").value("unpaid"))
                .andExpect(jsonPath("$.data.paid_amount_cent").value(3000))
                .andExpect(jsonPath("$.data.remaining_cent").value(7000));
        assertThat(f.cardBalance()).isEqualTo(17_000L);
        assertThat(f.order(orderId)).containsEntry("pay_status", "unpaid").containsEntry("issue_status", "not_issued");
        assertThat(f.entitlements(orderId)).isZero();
        f.payWithStoredValue(orderId, 7001L).andExpect(status().isBadRequest());
    }

    @Test
    void onlineRemainderCompletesAndIssues() throws Exception {
        String orderId = f.createOrderId(2);
        f.payWithStoredValue(orderId, 3000L).andExpect(status().isOk());
        String intentId = CustomerTicketPaymentFixture.read(f.startOnlinePayment(orderId, "wechat")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.amount_cent").value(7000)), "$.data.payment_intent_id");
        f.markPaid(intentId);
        f.confirmOnlinePayment(orderId, intentId).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pay_status").value("paid"))
                .andExpect(jsonPath("$.data.remaining_cent").value(0));
        assertThat(f.order(orderId)).containsEntry("pay_status", "paid").containsEntry("issue_status", "issued");
        assertThat(f.entitlements(orderId)).isEqualTo(2);
        f.listOrders().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].paid_amount_cent").value(10_000))
                .andExpect(jsonPath("$.data.items[0].payments.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].payments[0].channel").value("stored_value"))
                .andExpect(jsonPath("$.data.items[0].payments[1].channel").value("wechat"));
    }

    /** 储值 3000＋在线 7000、两张票各 5000：先退一张全落在后收的在线笔；再退一张先退在线剩 2000、再退储值 3000 回原卡。 */
    @Test
    void partialRefundTakesLastPaymentFirst() throws Exception {
        String orderId = f.createOrderId(2);
        f.payWithStoredValue(orderId, 3000L).andExpect(status().isOk());
        String intentId = CustomerTicketPaymentFixture.read(f.startOnlinePayment(orderId, "wechat")
                .andExpect(status().isOk()), "$.data.payment_intent_id");
        f.markPaid(intentId);
        f.confirmOnlinePayment(orderId, intentId).andExpect(status().isOk());
        assertThat(f.cardBalance()).isEqualTo(17_000L);
        List<String> tickets = jdbc.queryForList("SELECT id FROM ticket_instances WHERE order_id = ? ORDER BY seq_in_order",
                String.class, orderId);
        String entity = "be_b14split_" + f.suffix;
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, '退票责任主体', 'project_company', 'active', now(), now())", entity);

        TicketRefundRecord first = approve(refund(orderId, tickets.get(0), entity, "r1"));
        assertThat(first.status()).isEqualTo("approved");
        assertThat(f.cardBalance()).isEqualTo(17_000L);
        assertThat(payoutPending(first.id())).containsExactly("wechat:5000");

        TicketRefundRecord second = approve(refund(orderId, tickets.get(1), entity, "r2"));
        assertThat(second.status()).isEqualTo("approved");
        assertThat(f.cardBalance()).isEqualTo(20_000L);
        assertThat(payoutPending(second.id())).containsExactly("wechat:2000");
        // 财务确认拨付页按待打款明细打款：只打在线笔的 2000，储值 3000 已当场回卡，不能按整笔 5000 再打一次。
        assertThat(second.payoutItems()).containsExactly(new TicketRefundRecord.PayoutItem("wechat", 2000L));
        TestSecurityActors.asPlatformAdmin("usr_b14split_finance");
        assertThat(ticketingService.listTicketRefunds().items()).filteredOn(r -> r.id().equals(second.id()))
                .singleElement().satisfies(r -> {
                    assertThat(r.refundAmountCent()).isEqualTo(5000L);
                    assertThat(r.payoutItems()).containsExactly(new TicketRefundRecord.PayoutItem("wechat", 2000L));
                });
    }

    @Test
    void timedOutPartialOrderReturnsStoredValueToCard() throws Exception {
        String orderId = f.createOrderId(2);
        f.payWithStoredValue(orderId, 3000L).andExpect(status().isOk());
        assertThat(f.cardBalance()).isEqualTo(17_000L);
        ticketingService.cancelExpiredUnpaidTicketOrders(Instant.now().plusSeconds(60));
        assertThat(f.order(orderId)).containsEntry("status", "cancelled");
        assertThat(f.cardBalance()).isEqualTo(20_000L);
    }

    /**
     * 第十八批 BN 段第 261 条 261-B05-R1（PGM:303「同一个操作被重复提交，系统只认一次，不会因为重试就重复扣款、重复入账或重复核销」、
     * 行末「同一次操作重复提交时，返回第一次的结果并提示已完成。」；PGM:528「⑤同一笔操作重复提交不重复生效（重复点击、网络重发、渠道重复回调都只算一次）」）：
     * 储值付足后同一请求编号再提交，返回订单现状并提示已完成；只扣一次、只记一笔储值收款与一条扣款流水。
     */
    @Test
    void sameKeyResubmitAfterFullStoredValuePayRepliesCompletedWithoutSecondDebit() throws Exception {
        String orderId = f.createOrderId(2);
        String key = "b86sv-full-" + f.suffix;
        payStoredValue(orderId, key, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").doesNotExist())
                .andExpect(jsonPath("$.data.pay_status").value("paid"));
        payStoredValue(orderId, key, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.message").value(ApiResponse.REPLAYED_MESSAGE))
                .andExpect(jsonPath("$.data.pay_status").value("paid"));
        assertThat(f.cardBalance()).isEqualTo(10_000L);
        assertThat(storedValuePayments(orderId)).isEqualTo(1);
        assertThat(ticketDebits()).isEqualTo(1);
    }

    /** 261-B05-R1：只付一部分后同一请求编号再提交，同样提示已完成、不再扣款，订单仍待付、待付金额不变。 */
    @Test
    void sameKeyResubmitAfterPartialStoredValuePayRepliesCompletedWithoutSecondDebit() throws Exception {
        String orderId = f.createOrderId(2);
        String key = "b86sv-part-" + f.suffix;
        payStoredValue(orderId, key, 3000L).andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").doesNotExist());
        payStoredValue(orderId, key, 3000L).andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.message").value(ApiResponse.REPLAYED_MESSAGE))
                .andExpect(jsonPath("$.data.pay_status").value("unpaid"))
                .andExpect(jsonPath("$.data.remaining_cent").value(7000));
        assertThat(f.cardBalance()).isEqualTo(17_000L);
        assertThat(f.order(orderId)).containsEntry("pay_status", "unpaid");
        assertThat(storedValuePayments(orderId)).isEqualTo(1);
        assertThat(ticketDebits()).isEqualTo(1);
    }

    /**
     * 261-B05-R1（PGM:303「两个操作同时发生冲突时，一律以“先处理成功”的那个为准」及其括号内「其余自动失效并提示；支付回调、核销、充值到账同理」）：
     * 订单已在线付清后客人再点储值支付（新请求编号），本次不生效并提示订单不在待支付状态、不扣款；再点一次同样。
     */
    @Test
    void storedValuePayAfterOrderPaidOnlineIsRejectedWithoutDebit() throws Exception {
        String orderId = f.createOrderId(2);
        String intentId = CustomerTicketPaymentFixture.read(f.startOnlinePayment(orderId, "wechat")
                .andExpect(status().isOk()), "$.data.payment_intent_id");
        f.markPaid(intentId);
        f.confirmOnlinePayment(orderId, intentId).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pay_status").value("paid"));
        for (int attempt = 0; attempt < 2; attempt++) {
            f.payWithStoredValue(orderId, null).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail_code").value("ORDER_NOT_PAYABLE"));
            assertThat(f.cardBalance()).isEqualTo(20_000L);
        }
        assertThat(ticketDebits()).isZero();
    }

    /** 储值付款，请求编号由用例指定（customerPost 把同一编号放进 Idempotency-Key 头，与客人端同一次操作沿用编号一致）。 */
    private ResultActions payStoredValue(String orderId, String key, Long amountCent) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("account_id", f.accountId);
        body.put("phone", f.phone);
        body.put("sms_code", "000000");
        if (amountCent != null) {
            body.put("amount_cent", amountCent);
        }
        return f.customerPost("/api/v1/customer/orders/" + orderId + "/stored-value/pay", body);
    }

    private int storedValuePayments(String orderId) {
        return jdbc.queryForObject("SELECT count(*) FROM ticket_payment_intents WHERE business_order_id = ?"
                + " AND channel = 'stored_value' AND status = 'succeeded'", Integer.class, orderId);
    }

    private int ticketDebits() {
        return jdbc.queryForObject("SELECT count(*) FROM customer_stored_value_transactions WHERE account_id = ?"
                + " AND left(idempotency_key, 9) = 'ticketsv_'", Integer.class, f.accountId);
    }

    private TicketRefundRecord refund(String orderId, String ticketId, String entity, String tag) {
        TestSecurityActors.asPlatformAdmin("usr_b14split_requester");
        return refundService.requestRefund(new TicketRefundRequest("b14split-" + tag + "-" + f.suffix, "refund", List.of(),
                orderId, null, "客人退票", "original", null, List.of(ticketId), "service_unavailable", entity));
    }

    private TicketRefundRecord approve(TicketRefundRecord requested) {
        TestSecurityActors.asPlatformAdmin("usr_b14split_approver");
        return refundService.approveRefund(requested.id(),
                new StatusChangeRequest("b14split-a-" + requested.id(), "approve", "审批通过", List.of()));
    }

    private List<String> payoutPending(String refundId) {
        return jdbc.queryForList("SELECT metadata->>'refund_channel' || ':' || (metadata->>'refund_amount_cent') FROM audit_events"
                + " WHERE event_name = 'finance.ticket_refund.payout_pending' AND business_id = ? ORDER BY occurred_at", String.class, refundId);
    }
}
