package com.complex.platform.customer;

// 文件版本：V0.01

import static com.complex.platform.support.CustomerProjectSwitchRequests.keyed;
import static com.complex.platform.support.CustomerProjectSwitchRequests.rejected;
import static com.complex.platform.support.CustomerTicketPaymentFixture.read;
import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * PGM 第 736 行「可以按全局、综合体、商户、业务系统、模块、版本/设备、灰度开关」、第 744 行「前端隐藏只是辅助，后端接口必须独立校验」：
 * 有两个综合体时关掉甲的客人端，甲的票、订单与转赠在服务端按所属订单的综合体判——不带 X-Project-Id 或把头写成乙都 403 TERMINAL_DISABLED，
 * 写操作在幂等之前判（开着时成功过的同一请求编号，关掉后重放也拒）；乙照常。
 * 涉及：我的票（订单内票实例）、取入场动态码、票的使用记录、门票转赠发起/领取/撤回/预览、在线付款发起与确认、储值付款、
 * 专属入口票种列表（带 bar_id）与门票奖励规则公告。真库、真接口，客人请求按客人端真实形状；app、miniapp 翻成 enforce（与生产一致）。
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
class CustomerTicketProjectSwitchJdbcIntegrationTests {

    private static final String TRANSFERS = "/api/v1/customer/ticket-transfers";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @MockitoBean private PaymentGatewayRouter gateway;

    /** 甲＝被关的综合体，乙＝另一个综合体；各一张已发布票种、一位客人与一张 20000 分的本项目储值卡。 */
    private CustomerTicketPaymentFixture a;
    private CustomerTicketPaymentFixture b;
    private CustomerProjectSwitchRequests req;

    @BeforeEach
    void seed() {
        a = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 20_000L);
        b = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 20_000L);
        b.stubGateway(gateway); // 渠道替身对两个综合体的付款单都生效（markPaid 一律经 b）
        req = new CustomerProjectSwitchRequests(mockMvc, jdbc, json);
    }

    @Test
    void orderTicketsQrAndUsagesFollowOrderProject() throws Exception {
        String aOrder = paidOrder(a);
        String bOrder = paidOrder(b);
        String aTicket = entitlement(aOrder);
        String bTicket = entitlement(bOrder);
        req.close(a.projectId);

        req.get("/api/v1/customer/orders/" + bOrder + "/tickets", b.token, null).andExpect(status().isOk());
        req.get("/api/v1/customer/tickets/" + bTicket + "/qr", b.token, null).andExpect(status().isOk());
        req.get("/api/v1/customer/tickets/" + bTicket + "/usages", b.token, null).andExpect(status().isOk());

        rejected(b.projectId, h -> req.get("/api/v1/customer/orders/" + aOrder + "/tickets", a.token, h));
        rejected(b.projectId, h -> req.get("/api/v1/customer/tickets/" + aTicket + "/qr", a.token, h));
        rejected(b.projectId, h -> req.get("/api/v1/customer/tickets/" + aTicket + "/usages", a.token, h));
    }

    /** 储值付门票：按订单综合体判，先于扣款与幂等。 */
    @Test
    void storedValuePayFollowsOrderProject() throws Exception {
        String aOrder = a.createOrderId(1);
        Map<String, Object> pay = storedValuePay(a);
        req.post(storedValuePath(aOrder), a.token, null, pay).andExpect(status().isOk());
        String bOrder = b.createOrderId(1);
        req.close(a.projectId);

        req.post(storedValuePath(bOrder), b.token, null, storedValuePay(b))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pay_status").value("paid"));

        rejected(b.projectId, h -> req.post(storedValuePath(aOrder), a.token, h, pay));
        assertThat(a.cardBalance()).isEqualTo(15_000L);
    }

    /** 按综合体单独停储值消费模块（customer.stored_value.consume）：储值付票同样按订单综合体在服务端判，不认 X-Project-Id。 */
    @Test
    void storedValuePayModuleSwitchFollowsOrderProject() throws Exception {
        String aOrder = a.createOrderId(1);
        String bOrder = b.createOrderId(1);
        req.close(a.projectId, "customer.stored_value.consume");

        req.post(storedValuePath(bOrder), b.token, null, storedValuePay(b))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pay_status").value("paid"));

        rejected(b.projectId, h -> req.post(storedValuePath(aOrder), a.token, h, storedValuePay(a)));
        assertThat(a.cardBalance()).isEqualTo(20_000L);
    }

    @Test
    void onlinePaymentStartAndConfirmFollowOrderProject() throws Exception {
        String aOrder = a.createOrderId(1);
        Map<String, Object> start = keyed("b16-pay", "channel", "wechat");
        String aIntent = read(req.post(paymentsPath(aOrder), a.token, null, start).andExpect(status().isOk()), "$.data.payment_intent_id");
        b.markPaid(aIntent);
        Map<String, Object> confirm = keyed("b16-pay-confirm");
        req.post(paymentsPath(aOrder) + "/" + aIntent + "/confirm", a.token, null, confirm).andExpect(status().isOk());
        String bOrder = b.createOrderId(1);
        req.close(a.projectId);

        String bIntent = read(req.post(paymentsPath(bOrder), b.token, null, keyed("b16-pay", "channel", "wechat"))
                .andExpect(status().isOk()), "$.data.payment_intent_id");
        b.markPaid(bIntent);
        req.post(paymentsPath(bOrder) + "/" + bIntent + "/confirm", b.token, null, keyed("b16-pay-confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pay_status").value("paid"));

        rejected(b.projectId, h -> req.post(paymentsPath(aOrder), a.token, h, start));
        rejected(b.projectId, h -> req.post(paymentsPath(aOrder) + "/" + aIntent + "/confirm", a.token, h, confirm));
    }

    /** 门票转赠发起、领取、撤回、预览：按所转票的订单综合体判；转赠不冻结，领到的人可再转。 */
    @Test
    void ticketTransfersFollowTicketProject() throws Exception {
        String aParent = parentTicket(paidOrder(a));
        String aReceiver = CustomerProjectSwitchRequests.customer(customers, "recv");
        Map<String, Object> initiate = keyed("b16-tt", "parent_ticket_id", aParent, "slot_index", 1, "channel", "link");
        ResultActions first = req.post(TRANSFERS, a.token, null, initiate).andExpect(status().isOk());
        String firstId = read(first, "$.data.id");
        Map<String, Object> claim = keyed("b16-tt-claim", "claim_code", read(first, "$.data.claim_code"));
        req.post(TRANSFERS + "/" + firstId + "/claim", aReceiver, null, claim).andExpect(status().isOk());
        ResultActions pending = req.post(TRANSFERS, aReceiver, null,
                keyed("b16-tt", "parent_ticket_id", aParent, "slot_index", 1, "channel", "link")).andExpect(status().isOk());
        String bParent = parentTicket(paidOrder(b));
        String bReceiver = CustomerProjectSwitchRequests.customer(customers, "recv");
        req.close(a.projectId);

        ResultActions bFirst = req.post(TRANSFERS, b.token, null,
                keyed("b16-tt", "parent_ticket_id", bParent, "slot_index", 1, "channel", "link")).andExpect(status().isOk());
        req.get(TRANSFERS + "/preview", bReceiver, null, "claim_code", read(bFirst, "$.data.claim_code")).andExpect(status().isOk());
        req.post(TRANSFERS + "/" + read(bFirst, "$.data.id") + "/revoke", b.token, null, Map.of()).andExpect(status().isOk());
        ResultActions bSecond = req.post(TRANSFERS, b.token, null,
                keyed("b16-tt", "parent_ticket_id", bParent, "slot_index", 1, "channel", "link")).andExpect(status().isOk());
        req.post(TRANSFERS + "/" + read(bSecond, "$.data.id") + "/claim", bReceiver, null,
                keyed("b16-tt-claim", "claim_code", read(bSecond, "$.data.claim_code"))).andExpect(status().isOk());

        rejected(b.projectId, h -> req.post(TRANSFERS, a.token, h, initiate));
        rejected(b.projectId, h -> req.post(TRANSFERS + "/" + firstId + "/claim", aReceiver, h, claim));
        rejected(b.projectId, h -> req.get(TRANSFERS + "/preview", a.token, h, "claim_code", read(pending, "$.data.claim_code")));
        rejected(b.projectId, h -> req.post(TRANSFERS + "/" + read(pending, "$.data.id") + "/revoke", aReceiver, h, Map.of()));
    }

    /** 匿名可读：专属入口（带 bar_id）按该酒吧的综合体判，不带 bar_id 的公共列表不属单一综合体不判；奖励规则公告给了综合体按综合体、只给票种按票种判。 */
    @Test
    void ticketProductsByBarAndRewardRulesFollowProject() throws Exception {
        String aBar = req.barMerchant(a.projectId, "a");
        String bBar = req.barMerchant(b.projectId, "b");
        req.close(a.projectId);

        req.get("/api/v1/customer/ticket-products", null, null, "bar_id", bBar).andExpect(status().isOk());
        req.get("/api/v1/customer/ticket-products", null, null).andExpect(status().isOk());
        req.get("/api/v1/customer/ticket-reward-rules", null, null, "project_id", b.projectId).andExpect(status().isOk());
        req.get("/api/v1/customer/ticket-reward-rules", null, null, "ticket_type_id", b.ticketTypeId).andExpect(status().isOk());
        req.get("/api/v1/customer/ticket-reward-rules", null, null).andExpect(status().isOk());

        rejected(b.projectId, h -> req.get("/api/v1/customer/ticket-products", null, h, "bar_id", aBar));
        rejected(b.projectId, h -> req.get("/api/v1/customer/ticket-reward-rules", null, h, "project_id", a.projectId));
        rejected(b.projectId, h -> req.get("/api/v1/customer/ticket-reward-rules", null, h, "ticket_type_id", a.ticketTypeId));
        // 两者都带：project_id 就是公告查询的过滤条件，按它判
        rejected(b.projectId, h -> req.get("/api/v1/customer/ticket-reward-rules", null, h,
                "ticket_type_id", b.ticketTypeId, "project_id", a.projectId));
    }

    // ---------- helpers ----------

    /** 下单并用储值卡付清（开关开着时），付足即出票。 */
    private String paidOrder(CustomerTicketPaymentFixture f) throws Exception {
        String orderId = f.createOrderId(1);
        req.post(storedValuePath(orderId), f.token, null, storedValuePay(f)).andExpect(status().isOk());
        return orderId;
    }

    private String entitlement(String orderId) {
        return jdbc.queryForObject("SELECT id FROM issued_entitlements WHERE order_id = ? ORDER BY id LIMIT 1", String.class, orderId);
    }

    private String parentTicket(String orderId) {
        return jdbc.queryForObject("SELECT id FROM ticket_instances WHERE order_id = ? ORDER BY id LIMIT 1", String.class, orderId);
    }

    private static Map<String, Object> storedValuePay(CustomerTicketPaymentFixture f) {
        return keyed("b16-sv", "account_id", f.accountId, "phone", f.phone, "sms_code", "000000");
    }

    private static String storedValuePath(String orderId) {
        return "/api/v1/customer/orders/" + orderId + "/stored-value/pay";
    }

    private static String paymentsPath(String orderId) {
        return "/api/v1/customer/orders/" + orderId + "/payments";
    }
}
