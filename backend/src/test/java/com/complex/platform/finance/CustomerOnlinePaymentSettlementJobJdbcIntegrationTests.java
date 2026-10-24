package com.complex.platform.finance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.ticketing.TicketingService;
import java.time.Instant;
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
 * ROOT-03-R18（BSM 第 859 行「客人付款成功后必须出票……支付渠道会校验出票状态」；BSM 第 1553 行「各渠道（微信、支付宝等）的支付与退款结果、
 * 流水号、入账口径和对账明细」）：客人付完款不回前台、客户端被杀或断网，服务端结算作业也向渠道查单并入账出票；渠道未启用整轮跳过；
 * 超时取消作业取消前先查单，已付的出票不取消。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class CustomerOnlinePaymentSettlementJobJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private CustomerOnlinePaymentSettlementJob job;
    @Autowired private TicketingService ticketingService;
    @MockitoBean private PaymentGatewayRouter gateway;

    private CustomerTicketPaymentFixture f;

    @BeforeEach
    void seed() {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        f.stubGateway(gateway);
    }

    @Test
    void clientNeverConfirmsJobStillIssuesTickets() throws Exception {
        String orderId = f.createOrderId(1);
        String intentId = paidButNeverConfirmed(orderId);
        job.settlePending();
        assertThat(f.order(orderId)).containsEntry("pay_status", "paid").containsEntry("issue_status", "issued");
        assertThat(f.entitlements(orderId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM finance_payment_intents WHERE id = ?", String.class, intentId))
                .isEqualTo("succeeded");
    }

    @Test
    void jobSkipsWhenGatewayDisabled() throws Exception {
        String orderId = f.createOrderId(1);
        String intentId = paidButNeverConfirmed(orderId);
        when(gateway.isGatewayEnabled()).thenReturn(false);
        job.settlePending();
        assertThat(f.order(orderId)).containsEntry("pay_status", "unpaid");
        assertThat(jdbc.queryForObject("SELECT status FROM finance_payment_intents WHERE id = ?", String.class, intentId))
                .isEqualTo("created");
    }

    @Test
    void expiryJobSettlesPaidIntentInsteadOfCancelling() throws Exception {
        String orderId = f.createOrderId(1);
        paidButNeverConfirmed(orderId);
        ticketingService.cancelExpiredUnpaidTicketOrders(Instant.now().plusSeconds(60));
        assertThat(f.order(orderId)).containsEntry("status", "created").containsEntry("pay_status", "paid")
                .containsEntry("issue_status", "issued");
    }

    /** 客人在渠道付完款即离开：渠道单已成功，客户端从未调确认；作业只看 30 秒前创建的待付单。 */
    private String paidButNeverConfirmed(String orderId) throws Exception {
        String intentId = CustomerTicketPaymentFixture.read(f.startOnlinePayment(orderId, "wechat").andExpect(status().isOk()),
                "$.data.payment_intent_id");
        f.markPaid(intentId);
        jdbc.update("UPDATE finance_payment_intents SET created_at = created_at - INTERVAL '5 minutes' WHERE id = ?", intentId);
        return intentId;
    }
}
