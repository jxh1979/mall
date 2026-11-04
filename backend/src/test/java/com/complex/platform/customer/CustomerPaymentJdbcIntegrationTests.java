package com.complex.platform.customer;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.bar.BarReservationRecord;
import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03 clients-R4 客人本人订单在线支付发起与回前台查单确认（BSM 第 859 行「通过官方 App/小程序购票、预定、确认订单、查看支付结果」、
 * 第 860 行「订台需要支付定金」）＋ backend-R18 拉起参数形状（BSM 第 1552～1554 行聚合支付）：金额由服务端定、只许本人订单、
 * 渠道核验成功才算付款、渠道未启用不入账不出票、换渠道作废旧的待付单；发起响应带 launch，付款端取会话锁定的登录端。客人端请求按真实形状（蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class CustomerPaymentJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @MockitoBean private PaymentGatewayRouter gateway;

    private CustomerTicketPaymentFixture f;

    @BeforeEach
    void seed() {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        f.stubGateway(gateway);
    }

    @Test
    void createsTicketPaymentForOwnOrderWithServerAmountAndLaunch() throws Exception {
        String orderId = f.createOrderId(2);
        var result = f.startOnlinePayment(orderId, "wechat").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount_cent").value(10_000))
                .andExpect(jsonPath("$.data.gateway_status").value("PENDING"))
                .andExpect(jsonPath("$.data.launch.kind").value("miniapp_wechat"))
                .andExpect(jsonPath("$.data.launch.params.timeStamp").value("1700000000"));
        String intentId = CustomerTicketPaymentFixture.read(result, "$.data.payment_intent_id");
        assertThat(jdbc.queryForMap("SELECT business_order_type, amount_cent, channel, status FROM ticket_payment_intents WHERE id = ?",
                intentId)).containsEntry("business_order_type", "ticket_order").containsEntry("amount_cent", 10_000L)
                .containsEntry("channel", "wechat").containsEntry("status", "created");
        // R18：付款端取门禁派生的可信终端（本会话锁定的小程序），透传给渠道。
        assertThat(f.initiatedFor(intentId).terminalType()).isEqualTo("miniapp");
        assertThat(f.initiatedFor(intentId).businessOrderId()).isEqualTo(orderId);
    }

    @Test
    void otherCustomersOrderRejected() throws Exception {
        String orderId = f.createOrderId(1);
        Instant now = Instant.now();
        customers.saveCustomer(new CustomerRecord("cus_other_" + f.suffix, "13600000000", "136****0000", "active", null, null, now, now));
        customers.saveSession(new CustomerSessionRecord("sess_other_" + f.suffix, "cus_other_" + f.suffix, "tok_other_" + f.suffix,
                "miniapp", now, now.plusSeconds(3600), "active"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/customer/orders/" + orderId + "/payments")
                        .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", "tok_other_" + f.suffix)
                        .header("Idempotency-Key", "b14-other-" + f.suffix)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"b14-other-" + f.suffix + "\",\"channel\":\"wechat\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_ORDER_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_payment_intents WHERE business_order_id = ?", Integer.class, orderId))
                .isZero();
    }

    @Test
    void switchingChannelSupersedesPendingIntent() throws Exception {
        String orderId = f.createOrderId(1);
        String first = CustomerTicketPaymentFixture.read(f.startOnlinePayment(orderId, "wechat").andExpect(status().isOk()),
                "$.data.payment_intent_id");
        // 小程序只能微信付；换一笔新的待付单后旧单作废。
        f.startOnlinePayment(orderId, "alipay").andExpect(status().isBadRequest());
        String second = CustomerTicketPaymentFixture.read(f.startOnlinePayment(orderId, "wechat").andExpect(status().isOk()),
                "$.data.payment_intent_id");
        assertThat(second).isNotEqualTo(first);
        assertThat(jdbc.queryForObject("SELECT status FROM finance_payment_intents WHERE id = ?", String.class, first)).isEqualTo("voided");
        assertThat(jdbc.queryForObject("SELECT status FROM ticket_payment_intents WHERE id = ?", String.class, first)).isEqualTo("voided");
        assertThat(jdbc.queryForObject("SELECT status FROM finance_payment_intents WHERE id = ?", String.class, second)).isEqualTo("created");
    }

    @Test
    void relaunchAfterChannelPaidSettlesOldIntentInsteadOfVoiding() throws Exception {
        String orderId = f.createOrderId(1);
        String first = CustomerTicketPaymentFixture.read(f.startOnlinePayment(orderId, "wechat").andExpect(status().isOk()),
                "$.data.payment_intent_id");
        f.markPaid(first);
        // 客人在渠道已付款、App 没收到结果，回订单页又点了一次付款：旧单按到账出票，不作废、不再拉起新的一笔。
        f.startOnlinePayment(orderId, "wechat").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment_intent_id").value(first))
                .andExpect(jsonPath("$.data.gateway_status").value("SUCCEEDED"));
        assertThat(f.order(orderId)).containsEntry("pay_status", "paid").containsEntry("issue_status", "issued");
        assertThat(jdbc.queryForObject("SELECT status FROM finance_payment_intents WHERE id = ?", String.class, first)).isEqualTo("succeeded");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_payment_intents WHERE business_order_id = ?", Integer.class, orderId))
                .isEqualTo(1);
    }

    @Test
    void confirmWithGatewayDisabledLeavesOrderUnpaid() throws Exception {
        String orderId = f.createOrderId(1);
        String intentId = CustomerTicketPaymentFixture.read(f.startOnlinePayment(orderId, "wechat").andExpect(status().isOk()),
                "$.data.payment_intent_id");
        f.markPaid(intentId);
        when(gateway.isGatewayEnabled()).thenReturn(false);
        f.confirmOnlinePayment(orderId, intentId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PAYMENT_GATEWAY_UNAVAILABLE"));
        assertThat(f.order(orderId)).containsEntry("pay_status", "unpaid").containsEntry("issue_status", "not_issued");
        assertThat(f.entitlements(orderId)).isZero();
    }

    @Test
    void confirmBeforeGatewaySucceededLeavesOrderUnpaid() throws Exception {
        String orderId = f.createOrderId(1);
        String intentId = CustomerTicketPaymentFixture.read(f.startOnlinePayment(orderId, "wechat").andExpect(status().isOk()),
                "$.data.payment_intent_id");
        f.confirmOnlinePayment(orderId, intentId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PAYMENT_NOT_SUCCEEDED"));
        assertThat(f.order(orderId)).containsEntry("pay_status", "unpaid");
    }

    @Test
    void confirmAfterGatewaySucceededIssuesTicketsOnce() throws Exception {
        String orderId = f.createOrderId(2);
        String intentId = CustomerTicketPaymentFixture.read(f.startOnlinePayment(orderId, "wechat").andExpect(status().isOk()),
                "$.data.payment_intent_id");
        f.markPaid(intentId);
        f.confirmOnlinePayment(orderId, intentId).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pay_status").value("paid"))
                .andExpect(jsonPath("$.data.issue_status").value("issued"));
        f.confirmOnlinePayment(orderId, intentId).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.issue_status").value("issued"));
        assertThat(f.entitlements(orderId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT status FROM finance_payment_intents WHERE id = ?", String.class, intentId))
                .isEqualTo("succeeded");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'customer.payment_confirmed'"
                + " AND business_id = ?", Integer.class, intentId)).isEqualTo(2);
        // 第十八批 P 段第 132 条对照：门票线上付仍记一行平台自营内部核算收入。
        assertThat(jdbc.queryForList("SELECT counterparty_id FROM ar_ap_items WHERE direction = 'internal_accounting'"
                + " AND source_type = 'ticket_order' AND source_id = ?", String.class, orderId)).containsExactly("platform_self");
    }

    @Test
    void depositPaymentConfirmMarksDepositPaidWithChannel() throws Exception {
        Instant now = Instant.now();
        String reservationId = "reservation_b14pay_" + f.suffix;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, 'T-B14', '卡座', 'card_seat', 6, 2, 50000, 'CNY', 'active', now(), now())",
                "table_b14pay_" + f.suffix, "bar_b14pay_" + f.suffix);
        customers.saveCustomerBarReservation(new BarReservationRecord(reservationId, "bar_b14pay_" + f.suffix, null,
                "table_b14pay_" + f.suffix, f.customerId, now.plusSeconds(7200), 2, 2, 20_000L, "pending", "miniapp", null,
                "reserved", "miniapp", null, List.of(), now, now, null));
        String intentId = CustomerTicketPaymentFixture.read(f.customerPost(
                        "/api/v1/customer/bar-reservations/" + reservationId + "/deposit-payments",
                        Map.of("idempotency_key", "b14-dep-" + f.suffix, "channel", "wechat", "amount_cent", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount_cent").value(20_000))
                .andExpect(jsonPath("$.data.launch.kind").value("miniapp_wechat")), "$.data.payment_intent_id");
        f.markPaid(intentId);
        f.customerPost("/api/v1/customer/bar-reservations/" + reservationId + "/deposit-payments/" + intentId + "/confirm",
                        Map.of("idempotency_key", "b14-dep-confirm-" + f.suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deposit_status").value("paid"));
        assertThat(jdbc.queryForMap("SELECT deposit_status, deposit_channel FROM bar_reservations WHERE id = ?", reservationId))
                .containsEntry("deposit_status", "paid").containsEntry("deposit_channel", "wechat");
    }

    /**
     * 第十八批 P 段第 132 条（BSM:1062「客人自助订台必须支付定金，定金直接进该卡座所属的酒吧商户」；DEC-R3-2026-08-04-04）：
     * 客人线上付的订台定金收款主体是该酒吧商户（商户直收），到账确认不记平台自营内部核算收入。
     */
    @Test
    void onlineDepositToBarMerchantIsNotPlatformInternalAccountingRevenue() throws Exception {
        Instant now = Instant.now();
        String barId = "bar_p132_" + f.suffix;
        String tableId = "table_p132_" + f.suffix;
        String reservationId = "reservation_p132_" + f.suffix;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '定金入账测试酒吧', 'bar', 'active', now(), now(), ?)", barId, f.projectId);
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity,"
                + " allowed_pass_free_guest_count, min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'T-P132', '卡座', 'card_seat', 6, 2, 50000, 'CNY', 'active', now(), now())",
                tableId, barId, barId);
        customers.saveCustomerBarReservation(new BarReservationRecord(reservationId, barId, barId, tableId, f.customerId,
                now.plusSeconds(7200), 2, 2, 20_000L, "pending", "miniapp", null, "reserved", "miniapp", null, List.of(), now, now,
                null));
        String intentId = CustomerTicketPaymentFixture.read(f.customerPost(
                        "/api/v1/customer/bar-reservations/" + reservationId + "/deposit-payments",
                        Map.of("idempotency_key", "p132-dep-" + f.suffix, "channel", "wechat", "amount_cent", 1))
                .andExpect(status().isOk()), "$.data.payment_intent_id");
        f.markPaid(intentId);
        f.customerPost("/api/v1/customer/bar-reservations/" + reservationId + "/deposit-payments/" + intentId + "/confirm",
                        Map.of("idempotency_key", "p132-dep-confirm-" + f.suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deposit_status").value("paid"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE direction = 'internal_accounting'"
                + " AND source_type = 'bar_reservation_deposit' AND source_id = ?", Integer.class, reservationId)).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM finance_payment_intents WHERE id = ?", String.class, intentId))
                .isEqualTo("succeeded");
        assertThat(f.initiatedFor(intentId).payeeMerchantId()).isEqualTo(barId);
        assertThat(f.initiatedFor(intentId).fundMode()).isEqualTo("merchant_prepaid");
    }
}
