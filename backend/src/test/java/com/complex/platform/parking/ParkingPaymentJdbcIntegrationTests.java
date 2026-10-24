package com.complex.platform.parking;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.complex.platform.finance.gateway.PaymentGatewayRequest;
import com.complex.platform.finance.gateway.PaymentGatewayResult;
import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * HP-P01 R3 真库、真接口（客人端请求体蛇形键）：BSM L1435「出口扫码支付，或在官方 App/小程序提前缴费后在离场宽限时限内离场……
 * 不做车牌绑定无感支付」；L1433「无牌车……出口凭码缴费」；L1445「停车费收入归城市项目公司，进入收入归属与对账」。
 * 支付渠道用替身（真实渠道未接入），确认须向渠道核验成功回执。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        ParkingFixture.SECRET_PROPERTY
})
@AutoConfigureMockMvc
@Transactional
class ParkingPaymentJdbcIntegrationTests {

    private static final String GATEWAY = "pk_gateway";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customers;
    @Autowired private com.complex.platform.common.BusinessDayResolver businessDays;
    @MockitoBean private PaymentGatewayRouter gateway;

    private ParkingFixture f;
    private String token;
    private String company;
    private String session;
    private long due;

    @BeforeEach
    void seed() {
        f = new ParkingFixture(jdbc, "pay");
        f.policy();
        f.vendor(true);
        company = "pk_cpc_" + f.suffix;
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'城市项目公司','project_company','active',now(),now())",
                company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, f.project);
        Instant now = Instant.now();
        String customerId = "pk_cus_" + f.suffix;
        String phone = "139" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        customers.saveCustomer(new CustomerRecord(customerId, phone, "139****" + phone.substring(7), "active", null, null, now, now));
        token = "pk_tok_" + f.suffix;
        customers.saveSession(new CustomerSessionRecord("pk_sess_" + f.suffix, customerId, token, "miniapp", now, now.plusSeconds(3600), "active"));
        // 50 分钟前入场：免费 30 分后计费 20 分→1 小时×5 元；计费段跨营业日切点时两段各按 1 小时计。
        Instant entered = now.minus(Duration.ofMinutes(50));
        session = f.session("P1", "粤A12345", entered.toString());
        due = businessDays.businessDate(entered.plus(Duration.ofMinutes(30))).equals(businessDays.businessDate(now)) ? 500 : 1000;
        when(gateway.initiatePayment(any())).thenAnswer(inv -> new PaymentGatewayResult(PaymentGatewayResult.Status.PENDING, GATEWAY,
                "ref_" + UUID.randomUUID(), "pending", null, null,
                new com.complex.platform.finance.gateway.PaymentClientLaunch("miniapp_wechat", Map.of("timeStamp", "1700000000"))));
    }

    @Test
    void prepayConfirmSetsGraceUntil() throws Exception {
        lookup("粤a12345").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parking_session_id").value(session))
                .andExpect(jsonPath("$.data.due_cent").value(due));
        String paymentId = id(pay("k1_" + f.suffix).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount_cent").value(due))
                .andExpect(jsonPath("$.data.status").value("pending")));
        Instant before = Instant.now();
        confirm(paymentId).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("paid"));
        Map<String, Object> row = jdbc.queryForMap("SELECT paid_at, grace_until FROM parking_payments WHERE id=?", paymentId);
        Instant paid = ((java.sql.Timestamp) row.get("paid_at")).toInstant();
        Instant grace = ((java.sql.Timestamp) row.get("grace_until")).toInstant();
        assertThat(paid).isAfterOrEqualTo(before.minusSeconds(1));
        assertThat(Duration.between(paid, grace)).isEqualTo(Duration.ofMinutes(15));
        // 下发给停车系统的缴费记录带离场宽限截止。
        mockMvc.perform(f.openGet("/parking/payments")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].payment_id").value(paymentId))
                .andExpect(jsonPath("$.data[0].session_ref").value("P1"))
                .andExpect(jsonPath("$.data[0].grace_until").exists());
        // 收入进内部核算台账：来源停车费。
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_type='parking_fee' AND source_id=?",
                Integer.class, paymentId)).isEqualTo(1);
    }

    @Test
    void requestShapedLikeCustomerClient() throws Exception {
        mockMvc.perform(post("/api/v1/customer/parking/payments").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parking_session_id\":\"" + session + "\",\"idempotency_key\":\"cust-web-" + f.suffix + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parking_session_id").value(session))
                .andExpect(jsonPath("$.data.receiver_entity_id").value(company))
                // ROOT-03-R18：缴费发起响应带 App/小程序拉起参数（渠道原样签发）。
                .andExpect(jsonPath("$.data.launch.kind").value("miniapp_wechat"))
                .andExpect(jsonPath("$.data.launch.params.timeStamp").value("1700000000"));
    }

    /** BSM「员工车辆由所属主体登记车牌后，按后台配置免费或折扣」：停车系统标为员工车辆的，客人端应缴按本项目员工优惠（50%）计。 */
    @Test
    void staffVehicleQuoteAppliesStaffDiscount() throws Exception {
        jdbc.update("UPDATE parking_sessions SET vehicle_kind='staff' WHERE id=?", session);
        lookup("粤A12345").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fee_cent").value(due / 2))
                .andExpect(jsonPath("$.data.due_cent").value(due / 2));
    }

    @Test
    void paidSessionCannotPayAgain() throws Exception {
        confirm(id(pay("k2_" + f.suffix).andExpect(status().isOk()))).andExpect(status().isOk());
        pay("k3_" + f.suffix).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("PARKING_ALREADY_PAID"));
    }

    @Test
    void receiverIsCityProjectCompanyElseConflict() throws Exception {
        String second = "pk_cpc2_" + f.suffix;
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'第二家','project_company','active',now(),now())",
                second);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", second, f.project);
        pay("k4_" + f.suffix).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("CITY_PROJECT_COMPANY_MISSING"));
    }

    @Test
    void vendorPaymentMismatchCreatesReconciliationDifference() throws Exception {
        String paymentId = id(pay("k5_" + f.suffix).andExpect(status().isOk()));
        confirm(paymentId).andExpect(status().isOk());
        mockMvc.perform(f.webhook("payment_reported", "vp_" + f.suffix, Map.of("payment_ref", "VPX", "session_ref", "P1",
                "amount_cent", due + 100, "paid_at", Instant.now().toString(), "platform_payment_id", paymentId))).andExpect(status().isOk());
        Map<String, Object> diff = jdbc.queryForMap("""
                SELECT internal_amount_cent, provider_amount_cent, difference_amount_cent FROM integration_reconciliation_differences
                 WHERE provider_code=? AND external_business_id='VPX'""", f.provider);
        assertThat(((Number) diff.get("internal_amount_cent")).longValue()).isEqualTo(due);
        assertThat(((Number) diff.get("provider_amount_cent")).longValue()).isEqualTo(due + 100);
        assertThat(((Number) diff.get("difference_amount_cent")).longValue()).isEqualTo(100L);
    }

    private ResultActions lookup(String plate) throws Exception {
        return mockMvc.perform(get("/api/v1/customer/parking/sessions").param("project_id", f.project).param("plate_no", plate)
                .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token));
    }

    private ResultActions pay(String key) throws Exception {
        return mockMvc.perform(post("/api/v1/customer/parking/payments").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parking_session_id\":\"" + session + "\",\"idempotency_key\":\"" + key + "\"}"));
    }

    private ResultActions confirm(String paymentId) throws Exception {
        Map<String, Object> intent = jdbc.queryForMap("""
                SELECT i.id, i.gateway_ref, i.amount_cent FROM finance_payment_intents i
                  JOIN parking_payments p ON p.payment_intent_id = i.id WHERE p.id=?""", paymentId);
        String ref = (String) intent.get("gateway_ref");
        when(gateway.isGatewayEnabled()).thenReturn(true);
        when(gateway.queryStatus(ref)).thenReturn(new PaymentGatewayResult(PaymentGatewayResult.Status.SUCCEEDED, GATEWAY, ref, "", null,
                new PaymentGatewayRequest("parking_fee", paymentId, ((Number) intent.get("amount_cent")).longValue(), "CNY", "wechat",
                        ref, null, null)));
        return mockMvc.perform(post("/api/v1/customer/parking/payments/" + paymentId + "/confirm")
                .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token).contentType(MediaType.APPLICATION_JSON).content("{}"));
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }
}
