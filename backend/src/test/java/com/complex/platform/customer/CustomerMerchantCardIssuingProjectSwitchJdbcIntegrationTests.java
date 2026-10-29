package com.complex.platform.customer;

// 文件版本：V0.01

import static com.complex.platform.support.CustomerProjectSwitchRequests.keyed;
import static com.complex.platform.support.CustomerProjectSwitchRequests.rejected;
import static com.complex.platform.support.CustomerTicketPaymentFixture.read;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.complex.platform.support.CustomerProjectSwitchRequests;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 第十八批裁决 18（第 82 条 82-R18c）「客人端商户储值卡按发卡项目取开关」：商户 m（主项目甲）经审批加了经营项目乙（merchant_projects，
 * PGM 第 659 行「要经过审批才生效」），客人在乙开 m 的商户卡（首笔充值建卡，请求体 project_id＝乙、merchant_id＝m，卡上记下乙）。
 * 这张卡的充值、入账、查卡、逐笔记录与充值规则都按发卡项目乙判客人端开关：关甲照常；关乙则不带或把 X-Project-Id 写成开着的甲都 403
 * TERMINAL_DISABLED。卡上不带项目的 m 商户卡仍按主项目甲（不变）。真库、真接口，客人请求按客人端真实形状（蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "access.device_credential.enforcement.app=enforce",
        "access.device_credential.enforcement.miniapp=enforce"
})
@AutoConfigureMockMvc
@Transactional
class CustomerMerchantCardIssuingProjectSwitchJdbcIntegrationTests {

    private static final String TOPUP = "/api/v1/customer/stored-value/topup";
    private static final String SETTLE = "/api/v1/customer/stored-value/settle";
    private static final String ACCOUNT = "/api/v1/customer/stored-value/account";
    private static final String RULES = "/api/v1/customer/stored-value/topup-rules";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @MockitoBean private PaymentGatewayRouter gateway;

    /** 甲＝商户 m（a.merchantId）的主项目，乙＝m 经审批加的经营项目；客人是 a 的客人。 */
    private CustomerTicketPaymentFixture a;
    private CustomerTicketPaymentFixture b;
    private CustomerProjectSwitchRequests req;
    private Map<String, Object> topup;

    @BeforeEach
    void seed() {
        a = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        b = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        b.stubGateway(gateway);
        req = new CustomerProjectSwitchRequests(mockMvc, jdbc, json);
        jdbc.update("INSERT INTO merchant_projects (merchant_id, project_id) VALUES (?, ?)", a.merchantId, b.projectId);
        jdbc.update("INSERT INTO stored_value_merchant_refund_rules (merchant_id, card_issuance_enabled) VALUES (?, TRUE)", a.merchantId);
        topup = keyed("b27-r18c-topup", "project_id", b.projectId, "merchant_id", a.merchantId, "amount_cent", 5000, "channel", "wechat");
    }

    /** 关甲：m 在乙开的卡照常开卡充值、入账、查卡、看逐笔记录与充值规则；m 不带项目的卡仍按主项目甲被拒。 */
    @Test
    void merchantCardIssuedAtOperatingProjectIgnoresClosedMainProject() throws Exception {
        req.close(a.projectId);

        String card = issueAndSettle();
        req.get(ACCOUNT, a.token, null, "project_id", b.projectId, "merchant_id", a.merchantId)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(card));
        req.get(transactionsPath(card), a.token, null).andExpect(status().isOk());
        req.get(RULES, a.token, null, "project_id", b.projectId, "merchant_id", a.merchantId).andExpect(status().isOk());

        rejected(b.projectId, h -> req.get(ACCOUNT, a.token, h, "merchant_id", a.merchantId));
        rejected(b.projectId, h -> req.get(RULES, a.token, h, "merchant_id", a.merchantId));
    }

    /** 关乙：m 在乙开的卡，充值（同键重放）、入账、查卡、逐笔记录、充值规则不带或把头写成开着的甲都拒。 */
    @Test
    void closingIssuingProjectRejectsItsMerchantCard() throws Exception {
        String intent = read(req.post(TOPUP, a.token, null, topup).andExpect(status().isOk()), "$.data.id");
        b.markPaid(intent);
        Map<String, Object> settle = Map.of("project_id", b.projectId, "merchant_id", a.merchantId, "payment_intent_id", intent);
        String card = cardId();
        req.close(b.projectId);

        rejected(a.projectId, h -> req.post(TOPUP, a.token, h, topup));
        rejected(a.projectId, h -> req.post(SETTLE, a.token, h, settle));
        rejected(a.projectId, h -> req.get(ACCOUNT, a.token, h, "project_id", b.projectId, "merchant_id", a.merchantId));
        rejected(a.projectId, h -> req.get(transactionsPath(card), a.token, h));
        rejected(a.projectId, h -> req.get(RULES, a.token, h, "project_id", b.projectId, "merchant_id", a.merchantId));
    }

    private String issueAndSettle() throws Exception {
        String intent = read(req.post(TOPUP, a.token, null, topup).andExpect(status().isOk()), "$.data.id");
        b.markPaid(intent);
        req.post(SETTLE, a.token, null, Map.of("project_id", b.projectId, "merchant_id", a.merchantId, "payment_intent_id", intent))
                .andExpect(status().isOk());
        return cardId();
    }

    /** 首笔充值建的卡：卡上记下发卡项目乙。 */
    private String cardId() {
        return jdbc.queryForObject("SELECT id FROM customer_stored_value_accounts WHERE customer_id = ? AND project_id = ? AND merchant_id = ?",
                String.class, a.customerId, b.projectId, a.merchantId);
    }

    private static String transactionsPath(String accountId) {
        return "/api/v1/customer/stored-value/accounts/" + accountId + "/transactions";
    }
}
