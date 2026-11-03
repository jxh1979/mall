package com.complex.platform.gate;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.integration.ChannelTicketIssuePort;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 N 段第 123 条 123-R2（BSM:1630「核销、收款、退款、放行等敏感操作，操作设备定位落在该项目综合体授权范围之外时一律拒绝并留痕……」；
 * BSM:1876「Web 端同样适用」；第十八批裁决 30「拿不到位置按范围外处理」、裁决 32「没配就不查位置」）：敏感操作定位闸真库回归。
 * 先用 123-R1 接口给项目配圆心与 500 米半径，再按前端真实形状（蛇形键、终端头、X-Geo-Latitude/X-Geo-Longitude）走真实入口与授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SensitiveLocationGuardJdbcIntegrationTests {

    /** 圆心附近（约 70 米）与圆心以北约 2 公里。 */
    private static final String[] IN_RANGE = {"31.2310", "121.4740"};
    private static final String[] FAR = {"31.2484", "121.4737"};

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private ChannelTicketIssuePort channelIssue;

    private CustomerTicketPaymentFixture f;
    private StaffRequestFixture staffs;
    private String s;
    private RequestPostProcessor cashier;

    @BeforeEach
    void seed() throws Exception {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 20_000L);
        s = f.suffix;
        staffs = new StaffRequestFixture(jdbc);
        RequestPostProcessor configurer = staffs.staff("usr_r123g_cfg_" + s, "staff", f.projectId, null,
                "platform.sensitive_operation_range.manage");
        mockMvc.perform(put("/api/v1/platform/projects/" + f.projectId + "/sensitive-operation-range").with(configurer)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "r123g-range-" + s, "center_latitude", 31.2304, "center_longitude", 121.4737,
                                "radius_meters", 500, "min_radius_meters", 100, "max_radius_meters", 2000))))
                .andExpect(status().isOk());
        cashier = staffs.staff("usr_r123g_cash_" + s, "staff", f.projectId, f.merchantId, "customer.voucher.redeem");
    }

    // ① 范围内核销成功；② 范围外 403 SENSITIVE_OPERATION_OUT_OF_RANGE、券不动、独立留痕 out_of_range 且距离大于半径。
    @Test
    void voucherRedeemInRangeSucceedsOutOfRangeRejectedAndAudited() throws Exception {
        String inside = discountVoucher(f.merchantId);
        redeem(cashier, "r123g-in-" + s, redeemCode(inside, f.token), IN_RANGE).andExpect(status().isOk());
        assertThat(voucherStatus(inside)).isEqualTo("redeemed");

        String outside = discountVoucher(f.merchantId);
        redeem(cashier, "r123g-far-" + s, redeemCode(outside, f.token), FAR).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SENSITIVE_OPERATION_OUT_OF_RANGE"));
        assertThat(voucherStatus(outside)).isEqualTo("issued");
        Map<String, Object> audit = jdbc.queryForMap("SELECT metadata->>'result' AS result, metadata->>'reason' AS reason,"
                + " (metadata->>'distance_meters')::int AS distance, (metadata->>'radius_meters')::int AS radius"
                + " FROM audit_events WHERE event_name = 'sensitive_operation.location_rejected' AND business_id = 'redeemVoucher'"
                + " AND metadata->>'project_id' = ?", f.projectId);
        assertThat(audit).containsEntry("result", "failure").containsEntry("reason", "out_of_range").containsEntry("radius", 500);
        assertThat((Integer) audit.get("distance")).isGreaterThan(500);
    }

    // ③ 不带位置头（拿不到位置）按范围外处理：403 SENSITIVE_OPERATION_LOCATION_REQUIRED，留痕 location_missing。
    @Test
    void missingLocationRejected() throws Exception {
        String voucher = discountVoucher(f.merchantId);
        redeem(cashier, "r123g-none-" + s, redeemCode(voucher, f.token), null).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SENSITIVE_OPERATION_LOCATION_REQUIRED"));
        assertThat(voucherStatus(voucher)).isEqualTo("issued");
        assertThat(rejections("redeemVoucher", "location_missing")).isEqualTo(1);
    }

    // ④ 闸在幂等之前：已成功的幂等键换成范围外坐标重放仍 403。
    @Test
    void replayOutOfRangeStillRejected() throws Exception {
        String voucher = discountVoucher(f.merchantId);
        String code = redeemCode(voucher, f.token);
        String key = "r123g-replay-" + s;
        redeem(cashier, key, code, IN_RANGE).andExpect(status().isOk());
        redeem(cashier, key, code, FAR).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SENSITIVE_OPERATION_OUT_OF_RANGE"));
    }

    // ⑤ 没配授权范围的项目不查位置：不带位置头照常核销。
    @Test
    void unconfiguredProjectRedeemsWithoutLocation() throws Exception {
        CustomerTicketPaymentFixture other = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        RequestPostProcessor otherCashier = staffs.staff("usr_r123g_oc_" + s, "staff", other.projectId, other.merchantId,
                "customer.voucher.redeem");
        String voucher = discountVoucher(other.merchantId, other.customerId);
        redeem(otherCashier, "r123g-other-" + s, redeemCode(voucher, other.token), null).andExpect(status().isOk());
        assertThat(voucherStatus(voucher)).isEqualTo("redeemed");
    }

    // ⑥ 第三方通票回流登记渠道收款没有操作设备，不过定位闸：已配范围的项目不带位置照常出票、订单已支付。
    @Test
    void channelReflowConfirmsPaymentWithoutLocation() {
        TestSecurityActors.asPlatformAdmin();
        String orderId;
        try {
            orderId = channelIssue.issue(new ChannelTicketIssuePort.IssueCommand("r123g_channel_" + UUID.randomUUID(), f.ticketTypeId,
                    null, 1, LocalDate.now(), "meituan", f.phone, "美团回流")).ticketOrderId();
        } finally {
            TestSecurityActors.clear();
        }
        assertThat(jdbc.queryForObject("SELECT project_id FROM ticket_orders WHERE id = ?", String.class, orderId)).isEqualTo(f.projectId);
        assertThat(f.order(orderId)).containsEntry("pay_status", "paid");
    }

    // 票务核销范围外 403，权益不消耗。
    @Test
    void ticketVerificationOutOfRangeRejected() throws Exception {
        String orderId = f.createOrderId(1);
        f.payWithStoredValue(orderId, null).andExpect(status().isOk());
        String code = jdbc.queryForObject("SELECT entitlement_code FROM issued_entitlements WHERE order_id = ?", String.class, orderId);
        RequestPostProcessor gate = staffs.staff("usr_r123g_ver_" + s, "staff", f.projectId, f.merchantId, "verification.execute");
        perform(post("/api/v1/ticketing/verifications").with(gate), Map.of("idempotency_key", "r123g-ver-" + s,
                "credential_type", "entitlement_code", "credential_payload", code), FAR)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("SENSITIVE_OPERATION_OUT_OF_RANGE"));
        assertThat(jdbc.queryForObject("SELECT used_times FROM issued_entitlements WHERE order_id = ?", Integer.class, orderId)).isZero();
        assertThat(rejections("createTicketVerification", "out_of_range")).isEqualTo(1);
    }

    // 现场确认票款范围外 403，订单仍未支付。
    @Test
    void confirmTicketPaymentOutOfRangeRejected() throws Exception {
        String orderId = f.createOrderId(1);
        RequestPostProcessor teller = staffs.staff("usr_r123g_pay_" + s, "staff", f.projectId, null, "finance.payment.register");
        perform(post("/api/v1/ticketing/orders/" + orderId + "/confirm-payment").with(teller),
                Map.of("idempotency_key", "r123g-pay-" + s, "payment_intent_id", "pi_r123g_" + s), FAR)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("SENSITIVE_OPERATION_OUT_OF_RANGE"));
        assertThat(f.order(orderId).get("pay_status")).isNotEqualTo("paid");
        assertThat(rejections("confirmTicketOrderPayment", "out_of_range")).isEqualTo(1);
    }

    // 停车人工放行范围外 403，不落放行记录。
    @Test
    void parkingManualReleaseOutOfRangeRejected() throws Exception {
        RequestPostProcessor guard = staffs.staff("usr_r123g_park_" + s, "staff", f.projectId, null, "parking.release.record");
        perform(post("/api/v1/parking/manual-releases").with(guard), Map.of("idempotency_key", "r123g-park-" + s,
                "project_id", f.projectId, "plate_no", "沪A12345", "release_reason", "gate_fault", "reason", "道闸故障"), FAR)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("SENSITIVE_OPERATION_OUT_OF_RANGE"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_manual_releases WHERE project_id = ?", Integer.class, f.projectId))
                .isZero();
        assertThat(rejections("recordParkingManualRelease", "out_of_range")).isEqualTo(1);
    }

    // ---------- helpers ----------

    private ResultActions redeem(RequestPostProcessor who, String key, String code, String[] location) throws Exception {
        return perform(post("/api/v1/vouchers/redemptions").with(who), Map.of("idempotency_key", key, "code", code), location);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, Map<String, Object> body, String[] location) throws Exception {
        if (location != null) {
            request.header("X-Geo-Latitude", location[0]).header("X-Geo-Longitude", location[1]);
        }
        return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private int rejections(String operation, String reason) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'sensitive_operation.location_rejected'"
                + " AND business_id = ? AND metadata->>'reason' = ? AND metadata->>'project_id' = ?", Integer.class,
                operation, reason, f.projectId);
    }

    private String discountVoucher(String merchantId) {
        return discountVoucher(merchantId, f.customerId);
    }

    private String discountVoucher(String merchantId, String customerId) {
        String definition = "vdef_r123g_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO voucher_definitions (id, name, voucher_kind, redeem_method, settlement_mode, merchant_id, face_value_cent,"
                + " valid_days, status, transferable) VALUES (?, '折扣券', 'merchant_discount', 'in_store_staff', 'none', ?, 1000, 30,"
                + " 'active', TRUE)", definition, merchantId);
        String instance = "vi_r123g_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO voucher_instances (id, definition_id, customer_id, code, source_type, source_ref, status, issued_at,"
                + " expires_at) VALUES (?, ?, ?, ?, 'marketing_grant', 'vgrant_r123g', 'issued', now(), now() + interval '30 days')",
                instance, definition, customerId, "VC" + instance);
        return instance;
    }

    /** 客人券包出示的动态券码（客人端真实入口）。 */
    private String redeemCode(String voucher, String token) throws Exception {
        return CustomerTicketPaymentFixture.read(mockMvc.perform(get("/api/v1/customer/vouchers/" + voucher + "/redeem-code")
                .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token)).andExpect(status().isOk()), "$.data.code");
    }

    private String voucherStatus(String instance) {
        return jdbc.queryForObject("SELECT status FROM voucher_instances WHERE id = ?", String.class, instance);
    }
}
