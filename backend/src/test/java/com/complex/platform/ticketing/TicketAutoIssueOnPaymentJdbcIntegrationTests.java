package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest;
import java.time.Instant;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03-R17（BSM 第 859 行「客人付款成功后必须出票，不得出现付款成功而出票失败的情形，系统须从设计上避免此种情况发生；
 * 支付渠道会校验出票状态，确因出票失败的款项自动原路退回」）：付款成功与出票同一事务；储值付款出票失败整笔回滚不扣款；
 * 在线到账出票失败原路待退并取消订单；订单已超时取消后的到账一律原路待退、不再报错丢钱。客人端请求按真实形状（蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TicketAutoIssueOnPaymentJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private TicketOrderService orderService;
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
    void storedValuePaymentIssuesTicketsInSameTransaction() throws Exception {
        String orderId = f.createOrderId(2);
        f.payWithStoredValue(orderId, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pay_status").value("paid"))
                .andExpect(jsonPath("$.data.issue_status").value("issued"));
        assertThat(f.order(orderId)).containsEntry("pay_status", "paid").containsEntry("issue_status", "issued");
        assertThat(f.entitlements(orderId)).isEqualTo(2);
        assertThat(f.cardBalance()).isEqualTo(10_000L);
        f.listOrders().andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].issue_status").value("issued"));
    }

    /** 不在测试事务里跑，才看得到储值付款事务整笔回滚（造数带唯一后缀、按 id 隔离，留在一次性库中）。 */
    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void storedValuePaymentRollsBackWhenIssueFails() throws Exception {
        String orderId = f.createOrderId(1);
        // 票种各业态价格被清零：出票按业态拆账必然失败（TICKET_COMPONENT_PRICE_MISSING）。
        jdbc.update("UPDATE ticket_type_entitlements SET component_price_cent = 0 WHERE ticket_type_id = ?", f.ticketTypeId);
        f.payWithStoredValue(orderId, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_COMPONENT_PRICE_MISSING"));
        assertThat(f.cardBalance()).isEqualTo(20_000L);
        assertThat(f.order(orderId)).containsEntry("pay_status", "unpaid").containsEntry("issue_status", "not_issued");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_payment_intents WHERE business_order_id = ?",
                Integer.class, orderId)).isZero();
    }

    /** 订单超时取消后才确认到账（非全园故障）：不再抛 ORDER_CANCELLED，登记一笔原路待退。 */
    @Test
    void lateGatewayConfirmOnTimedOutOrderRecordsRefund() throws Exception {
        String orderId = f.createOrderId(1);
        TestSecurityActors.asPlatformAdmin();
        TicketPaymentIntentRecord intent = orderService.createPaymentIntent(orderId, new PaymentIntentRequest(
                "b14-late-" + f.suffix, null, List.of(), "ticket_order", orderId, 5000L, "CNY", "wechat"));
        orderService.cancelExpiredUnpaidOrders(Instant.now().plusSeconds(60));
        assertThat(f.order(orderId)).containsEntry("status", "cancelled");

        TicketOrderRecord result = orderService.confirmPayment(orderId,
                new ConfirmTicketOrderPaymentRequest("b14-late-confirm-" + f.suffix, intent.id(), "gw-late-" + f.suffix));

        assertThat(result.status()).isEqualTo("cancelled");
        Map<String, Object> refund = jdbc.queryForMap("SELECT status, refund_amount_cent, refund_channel, refund_direction"
                + " FROM finance_refunds WHERE source_type = 'ticket_payment_rejection' AND target_id = ?", intent.id());
        assertThat(refund).containsEntry("status", "requested").containsEntry("refund_amount_cent", 5000L)
                .containsEntry("refund_channel", "wechat").containsEntry("refund_direction", "original");
        assertThat(f.entitlements(orderId)).isZero();
    }

    /** 在线到账后出票失败：收款不回滚，登记原路待退并取消订单，客人端 409 TICKET_ISSUE_FAILED_REFUNDING。 */
    @Test
    void gatewayConfirmIssueFailureRefundsAndCancels() throws Exception {
        String orderId = f.createOrderId(1);
        String intentId = CustomerTicketPaymentFixture.read(
                f.startOnlinePayment(orderId, "wechat").andExpect(status().isOk()), "$.data.payment_intent_id");
        f.markPaid(intentId);
        jdbc.update("UPDATE ticket_type_entitlements SET component_price_cent = 0 WHERE ticket_type_id = ?", f.ticketTypeId);

        f.confirmOnlinePayment(orderId, intentId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_ISSUE_FAILED_REFUNDING"));

        assertThat(f.order(orderId)).containsEntry("status", "cancelled");
        assertThat(f.entitlements(orderId)).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM finance_payment_intents WHERE id = ?", String.class, intentId))
                .isEqualTo("succeeded");
        assertThat(jdbc.queryForMap("SELECT status, refund_amount_cent, refund_channel FROM finance_refunds"
                + " WHERE source_type = 'ticket_payment_rejection' AND target_id = ?", intentId))
                .containsEntry("status", "requested").containsEntry("refund_amount_cent", 5000L).containsEntry("refund_channel", "wechat");
    }
}
