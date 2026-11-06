package com.complex.platform.ticketing;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.complex.platform.support.TicketSalesContractTestFixtures;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 N 段第 120 条 120-R2（BSM:980「……其中只有启用售票能力的一体机，才允许授权服务人员在到店服务场景下辅助客人购票。」；BSM:986「启用售票能力的一体机可由
 * 授权服务人员登录后辅助客人购票和查询订单……在某个酒吧门口或前台售票时，只能售卖该酒吧的单店门票，不得售卖综合体通票。」；BSM:999「……售票一体机按设备点位和员工
 * 权限过滤可售票种，并把购票、支付、出票和售票原因写入留痕。」；BSM:1947「……一体机必须绑定点位、限制可售票种，并记录服务人员、设备、点位、购票原因、支付和出票。」；
 * DEC-R7-05「售票型触摸屏一体机能力补「订单查询」」）：管理员按后台真实请求把设备绑定为售票型一体机、所属商户设为酒吧 A、为本机开售票开关；授权服务人员在一体机上
 * （一体机终端头＋登记设备号）发绑定码、下单、登记现金、出票、按客人手机号查订单。请求按一体机真实形状（蛇形键）走真实入口与真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class KioskTicketSaleJdbcTests {

    private static final String[] SELLER_PERMISSIONS = {"ticket.order.create", "ticket.order.read", "finance.payment_intent.create",
            "finance.payment.register", "ticket.ticket.issue"};

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;

    private CustomerTicketPaymentFixture f;
    private String s;
    private String barA;
    private String barB;
    private String space;
    private String device;
    private String ticketA;
    private String ticketB;
    private RequestPostProcessor admin;
    private RequestPostProcessor seller;
    private RequestPostProcessor platformSeller;

    @BeforeEach
    void seed() throws Exception {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        s = f.suffix;
        barA = "bar_kts_a_" + s;
        barB = "bar_kts_b_" + s;
        space = "space_kts_" + s;
        device = "kts-kiosk-" + s;
        for (String bar : new String[] {barA, barB}) {
            jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                    + " VALUES (?, '一体机售票酒吧', 'bar', 'active', now(), now(), ?)", bar, f.projectId);
        }
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at)"
                + " VALUES (?, ?, '酒吧门口一体机', 'shop', 'active', now(), now())", space, f.projectId);
        ticketA = barTicket(barA);
        ticketB = barTicket(barB);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        admin = staffs.staff("usr_kts_admin_" + s, "platform_admin", f.projectId, null,
                "terminal.device.bind", "terminal.device.lifecycle", "terminal.switch.manage");
        seller = staffs.staff("usr_kts_seller_" + s, "staff", f.projectId, barA, SELLER_PERMISSIONS);
        platformSeller = staffs.staff("usr_kts_platform_" + s, "staff", f.projectId, null, SELLER_PERMISSIONS);
        // 管理员按后台真实请求把设备绑定为售票型一体机（按空间绑点位），设所属商户为酒吧 A。
        String row = CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/terminals/devices/bind").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "kts-bind-" + s, "device_id", device, "terminal_type", "kiosk",
                                "binding_scope", "space", "bound_space_id", space, "kiosk_mode", "ticketing"))))
                .andExpect(status().isOk()), "$.data.id");
        mockMvc.perform(put("/api/v1/terminals/devices/" + row + "/merchant").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "kts-merchant-" + s, "merchant_id", barA, "reason", "一体机所属酒吧"))))
                .andExpect(status().isOk());
    }

    @Test
    void ticketingKioskSellsOwnBarTicketRecordsReasonDeviceAndPointTakesCashIssuesAndFindsOrdersByPhone() throws Exception {
        switchTicketing(true);
        RequestPostProcessor atKiosk = kiosk(seller);
        mockMvc.perform(post("/api/v1/ticketing/kiosk-bind-codes").with(atKiosk).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", f.phone))))
                .andExpect(status().isOk());
        String key = "kts-order-" + s;
        ResultActions created = order(atKiosk, key, ticketA, barA, "kiosk", f.phone, "客人在酒吧门口补票").andExpect(status().isOk());
        String orderId = CustomerTicketPaymentFixture.read(created, "$.data.id");
        long amount = Long.parseLong(CustomerTicketPaymentFixture.read(created, "$.data.amount_cent"));
        // 170-R2（契约 TicketOrderRequest.bar_id 按代码声明）：单店票带所属酒吧下单，订单记下该酒吧。
        assertThat(jdbc.queryForObject("SELECT bar_id FROM ticket_orders WHERE id = ?", String.class, orderId)).isEqualTo(barA);
        assertThat(jdbc.queryForMap("SELECT metadata->>'sale_reason' AS sale_reason, metadata->>'device_id' AS device_id,"
                        + " metadata->>'point_id' AS point_id, metadata->>'terminal_type' AS terminal_type, actor_id FROM audit_events"
                        + " WHERE event_name = 'ticket_order.created' AND idempotency_key = ?", key))
                .containsEntry("sale_reason", "客人在酒吧门口补票").containsEntry("device_id", device)
                .containsEntry("point_id", space).containsEntry("terminal_type", "kiosk").containsEntry("actor_id", "usr_kts_seller_" + s);

        // 授权服务人员登记现金收款、出票。
        String intent = CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/ticketing/orders/" + orderId + "/pay-intents")
                        .with(atKiosk).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "kts-intent-" + s, "business_order_type", "ticket_order", "business_order_id", orderId,
                                "amount_cent", amount, "currency", "CNY", "channel", "cash"))))
                .andExpect(status().isOk()), "$.data.id");
        mockMvc.perform(post("/api/v1/ticketing/orders/" + orderId + "/confirm-payment").with(atKiosk)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "kts-confirm-" + s, "payment_intent_id", intent))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pay_status").value("paid"));
        mockMvc.perform(post("/api/v1/ticketing/orders/" + orderId + "/issue").with(atKiosk)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "kts-issue-" + s, "reason", "客人在酒吧门口补票"))))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT issue_status FROM ticket_orders WHERE id = ?", String.class, orderId)).isEqualTo("issued");

        // 订单查询：按客人手机号只列该号的订单，号码掩码；另一号的订单不出现。
        String other = "139" + f.phone.substring(3);
        mockMvc.perform(post("/api/v1/ticketing/kiosk-bind-codes").with(atKiosk).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", other))))
                .andExpect(status().isOk());
        order(atKiosk, "kts-order-other-" + s, ticketA, barA, "kiosk", other, "另一位客人补票").andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/ticketing/orders").param("buyer_phone", f.phone.substring(0, 3) + " " + f.phone.substring(3)).with(atKiosk))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(orderId))
                .andExpect(jsonPath("$.data.items[0].buyer_phone").value(f.phone.substring(0, 3) + "****" + f.phone.substring(7)));
    }

    @Test
    void kioskSaleNeedsTicketingKioskOnBothSidesReasonAndOwnBarSingleTicket() throws Exception {
        RequestPostProcessor atKiosk = kiosk(seller);
        // 本机还没有售票开关行：不开放售票，拒单不动库存。
        order(atKiosk, "kts-off-" + s, ticketA, barA, "kiosk", f.phone, "补票").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("KIOSK_TICKETING_DISABLED"));
        switchTicketing(true);
        // 不在一体机上却报一体机渠道：拒。
        order(seller, "kts-web-" + s, ticketA, barA, "kiosk", f.phone, "补票").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("KIOSK_TICKETING_DISABLED"));
        // 在一体机上却报后台渠道（后台渠道可免付出票）：拒。
        order(atKiosk, "kts-admin-" + s, ticketA, barA, "admin", f.phone, "补票").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("KIOSK_TICKETING_DISABLED"));
        // 不填售票原因：拒。
        order(atKiosk, "kts-noreason-" + s, ticketA, barA, "kiosk", f.phone, null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        // 没有商户范围的平台员工在酒吧 A 门口一体机上也不能卖综合体通票、酒吧 B 的单店票。
        RequestPostProcessor platformAtKiosk = kiosk(platformSeller);
        order(platformAtKiosk, "kts-pass-" + s, f.ticketTypeId, null, "kiosk", f.phone, "补票").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("BAR_SCOPE_FORBIDDEN"));
        order(platformAtKiosk, "kts-other-bar-" + s, ticketB, barB, "kiosk", f.phone, "补票").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("BAR_SCOPE_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_orders WHERE ticket_type_id IN (?, ?, ?)", Integer.class,
                ticketA, ticketB, f.ticketTypeId)).isZero();
        assertThat(jdbc.queryForObject("SELECT sold_quantity FROM ticket_types WHERE id = ?", Integer.class, ticketA)).isZero();
    }

    @Test
    void barStaffCannotSellThePassOnATicketingKioskWithoutOwnerMerchant() throws Exception {
        // 120-R2 复核：本机还没设所属商户时只按员工范围判（resolveOrderBarId）——酒吧 A 的员工在这台售票型一体机上按一体机渠道
        // 也不能卖综合体通票（BSM:986、BSM:1019），拒单不动库存。
        String bare = "kts-kiosk-bare-" + s;
        mockMvc.perform(post("/api/v1/terminals/devices/bind").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "kts-bind-bare-" + s, "device_id", bare, "terminal_type", "kiosk",
                                "binding_scope", "space", "bound_space_id", space, "kiosk_mode", "ticketing"))))
                .andExpect(status().isOk());
        switchTicketing(bare, true);
        RequestPostProcessor atBareKiosk = kiosk(seller, bare);
        mockMvc.perform(post("/api/v1/ticketing/kiosk-bind-codes").with(atBareKiosk).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", f.phone))))
                .andExpect(status().isOk());
        Integer sold = jdbc.queryForObject("SELECT sold_quantity FROM ticket_types WHERE id = ?", Integer.class, f.ticketTypeId);
        order(atBareKiosk, "kts-bare-pass-" + s, f.ticketTypeId, null, "kiosk", f.phone, "补票").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("BAR_SCOPE_FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("门店终端只能售卖本店单店票，不得售卖综合体通票"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_orders WHERE ticket_type_id = ?", Integer.class, f.ticketTypeId)).isZero();
        assertThat(jdbc.queryForObject("SELECT sold_quantity FROM ticket_types WHERE id = ?", Integer.class, f.ticketTypeId)).isEqualTo(sold);
    }

    @Test
    void adminWebIssueWithPageBodyReasonIssuesAndOperatorNoteOnlyIsRejected() throws Exception {
        // 第十八批 W 段 199-R1（契约 TicketIssueRequest：必填 idempotency_key、reason，additionalProperties false）：后台网页出票页按页面请求体
        // （adminClient 转蛇形后两键 idempotency_key、reason）出票；原先页面发 operator_note，后端不认、reason 为空，每次 400「原因未填写」。
        String orderId = CustomerTicketPaymentFixture.read(order(platformSeller, "kts-admin-web-" + s, ticketA, barA, "admin", f.phone, null)
                .andExpect(status().isOk()), "$.data.id");
        mockMvc.perform(post("/api/v1/ticketing/orders/" + orderId + "/issue").with(platformSeller)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "kts-admin-issue-note-" + s, "operator_note", "完整后台验收出票"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("原因未填写"));
        mockMvc.perform(post("/api/v1/ticketing/orders/" + orderId + "/issue").with(platformSeller)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "kts-admin-issue-" + s, "reason", "完整后台验收出票"))))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT issue_status FROM ticket_orders WHERE id = ?", String.class, orderId)).isEqualTo("issued");
    }

    // ---------- helpers ----------

    /** 一体机真实形状：一体机终端头＋登记设备号（不签名，默认 grace）。 */
    private RequestPostProcessor kiosk(RequestPostProcessor who) {
        return kiosk(who, device);
    }

    private RequestPostProcessor kiosk(RequestPostProcessor who, String deviceId) {
        return request -> {
            MockHttpServletRequest r = who.postProcessRequest(request);
            r.removeHeader("X-Terminal-Type");
            r.addHeader("X-Terminal-Type", "kiosk");
            r.addHeader("X-Device-Id", deviceId);
            return r;
        };
    }

    private void switchTicketing(boolean enabled) throws Exception {
        switchTicketing(device, enabled);
    }

    private void switchTicketing(String deviceId, boolean enabled) throws Exception {
        mockMvc.perform(put("/api/v1/terminals/capability-switches/tcs_kiosk_ticketing_" + deviceId).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "kts-switch-" + enabled + "-" + deviceId, "terminal_type", "kiosk", "capability_code", "ticketing",
                                "scope_id", deviceId, "enabled", enabled, "operator_note", "开启本机售票"))))
                .andExpect(status().isOk());
    }

    private ResultActions order(RequestPostProcessor who, String key, String ticketType, String barId, String channel, String phone,
            String reason) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("ticket_type_id", ticketType);
        body.put("quantity", 1);
        body.put("sales_channel", channel);
        body.put("buyer_phone", phone);
        body.put("sms_code", "000000");
        if (barId != null) {
            body.put("bar_id", barId);
        }
        if (reason != null) {
            body.put("operator_note", reason);
        }
        return mockMvc.perform(post("/api/v1/ticketing/orders").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    /** 已发布的酒吧单店门票（每张 8000 分，含一次入场权益），有已批准的销售合同。 */
    private String barTicket(String bar) {
        String type = "tt_kts_" + bar;
        String template = "et_kts_" + bar;
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,"
                + " holiday_price_cent, stock_required, status, validity_days, bar_id, created_at, updated_at)"
                + " VALUES (?, '酒吧门票', 'flat', 8000, 8000, 8000, FALSE, 'published', 1, ?, now(), now())", type, bar);
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,"
                + " merchant_id, entitlement_kind, consumption_window, created_at, updated_at)"
                + " VALUES (?, '酒吧入场', 'entry', 1, 8000, 'active', ?, 'virtual', 'per_use', now(), now())", template, bar);
        jdbc.update("INSERT INTO ticket_type_entitlements (id, ticket_type_id, entitlement_template_id, quantity, total_times,"
                + " num_people, value_amount_cent, component_price_cent, created_at, updated_at)"
                + " VALUES (?, ?, ?, 1, 1, 1, 8000, 8000, now(), now())", "bind_kts_" + bar, type, template);
        TicketSalesContractTestFixtures.approvedPublished(jdbc, json, type, f.projectId, bar);
        return type;
    }
}
