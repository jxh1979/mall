package com.complex.platform.bar;

// 文件版本：V0.06

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
 * 第十八批 N 段第 120 条 120-R4（BSM:988「支持吧台买酒下单：选择酒水、套餐、数量，生成待支付订单。」；BSM:1024「接收一体机产生的吧台买酒订单。」；
 * BSM:1611「订单从哪里来，是官方 App/小程序、第三方、销售人员、一体机还是现场工作台。」；BSM:1004「……一体机的下单、领取、状态检查和异常提示都要记录设备、点位和时间。」；
 * BSM:990「允许现场现金和 POS 机支付，但必须由授权员工记录到订单、账单、日结和对账里。」；BSM:1180「……一体机买酒或领取礼物后要回写库存消耗。」）：
 * 一体机常驻员工在本机酒吧吧台位下单——订单来源与创建终端记一体机、记本机设备号、不建出品票；生成待支付账单后由授权员工在一体机登记现金结清并留痕；
 * 吧台出品后扣吧台库存（既有路径）。管理员按后台真实请求绑定一体机，下单、生成账单、结账按一体机真实形状（一体机终端头＋登记设备号、蛇形键）走真实入口与真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class KioskBarOrderJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "kbo_prj_" + s;
    private final String bar = "kbo_bar_" + s;
    private final String table = "kbo_table_" + s;
    private final String device = "kbo-kiosk-" + s;
    private final String item = "kbo_item_" + s;
    private final String location = "kbo_loc_" + s;

    private RequestPostProcessor resident;
    private RequestPostProcessor cashier;
    private RequestPostProcessor barStaff;
    private String cashierId;
    private RequestPostProcessor webCashier;
    private String webCashierId;
    private String product;

    @BeforeEach
    void seed() throws Exception {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','一体机买酒综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'一体机买酒酒吧','bar','active',?,now(),now())",
                bar, project);
        String space = "kbo_space_" + s;
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at)"
                + " VALUES (?, ?, '吧台一体机', 'shop', 'active', now(), now())", space, project);
        String counter = "kbo_counter_" + s;
        jdbc.update("INSERT INTO warehouses (id, project_id, merchant_id, warehouse_type, name) VALUES (?, ?, ?, 'bar_counter', '一号吧台')", counter, project, bar);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, merchant_id, spec_volume_ml, created_at, updated_at)"
                + " VALUES (?, '某某啤酒 330 毫升瓶装', '酒类', '瓶', 'active', ?, 330, now(), now())", item, bar);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id)"
                + " VALUES (?, ?, ?, 10, 0, 10, now(), ?, ?)", "kbo_bal_" + s, item, location, bar, counter);
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, '吧台位', 'bar_counter', 4, 0, 0, 'CNY', 'active', now(), now())", table, bar, bar, "BC-" + s);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        RequestPostProcessor admin = staffs.staff("usr_kbo_admin_" + s, "platform_admin", project, null, "terminal.device.bind");
        // 一体机常驻员工：只有点单（下单、生成账单）权限。
        resident = staffs.staff("usr_kbo_resident_" + s, "staff", project, bar, "bar.order.create");
        // 授权员工：在一体机上当场登录登记现金。
        cashierId = "usr_kbo_cashier_" + s;
        cashier = staffs.staff(cashierId, "staff", project, bar, "finance.payment.register");
        // 第十八批 O 段第 127 条 127-R3：后台网页收银员——开班、下单出账、结账、交班。
        webCashierId = "usr_kbo_webcashier_" + s;
        webCashier = staffs.staff(webCashierId, "staff", project, bar,
                "bar.shift.manage", "bar.shift.close", "bar.order.create", "finance.payment.register");
        // 吧台员工：建商品、出品。
        barStaff = staffs.staff("usr_kbo_bar_" + s, "staff", project, bar, "bar.product.manage", "bar.order.create");
        // 管理员按后台真实请求把设备绑定为服务型一体机（按空间绑点位）。
        mockMvc.perform(post("/api/v1/terminals/devices/bind").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "kbo-bind-" + s, "device_id", device,
                                "terminal_type", "kiosk", "binding_scope", "space", "bound_space_id", space, "kiosk_mode", "service"))))
                .andExpect(status().isOk());
        // 后台商品管理「新建商品」按蛇形键建：按瓶出品、挂吧台库存行扣库。
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "kbo-product-" + s);
        body.put("bar_id", bar);
        body.put("product_name", "某某啤酒（整瓶）");
        body.put("category", "beer");
        body.put("price_cent", 5000);
        body.put("status", "active");
        body.put("stock_deduction_required", true);
        body.put("inventory_item_id", item);
        body.put("stock_location_id", location);
        product = data(mockMvc.perform(post("/api/v1/bar/products").with(barStaff).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isOk())).path("id").asText();
    }

    @Test
    void kioskBarCounterOrderRecordsKioskSourceAndDeviceThenCashierSettlesBillAndDeliveryDeductsStock() throws Exception {
        String order = data(order(kiosk(resident), "kiosk")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order_source").value("kiosk"))
                .andExpect(jsonPath("$.data.created_terminal").value("kiosk"))
                .andExpect(jsonPath("$.data.device_id").value(device))).path("id").asText();
        assertThat(jdbc.queryForMap("SELECT order_source, created_terminal, device_id, total_amount_cent FROM bar_orders WHERE id = ?", order))
                .containsEntry("order_source", "kiosk").containsEntry("created_terminal", "kiosk").containsEntry("device_id", device)
                .containsEntry("total_amount_cent", 10_000L);
        // 吧台位即点即取，不建出品票。
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_production_tickets WHERE order_id = ?", Integer.class, order)).isZero();

        // 生成待支付账单（常驻会话）。
        JsonNode bill = data(mockMvc.perform(post("/api/v1/bar/orders/" + order + "/bill").with(kiosk(resident))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "kbo-bill-" + s))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payable_amount_cent").value(10_000))
                .andExpect(jsonPath("$.data.payment_status").value("unpaid")));
        String billId = bill.path("id").asText();

        // 一体机收款步骤：授权员工登记现金结清（付款凭据编号取本次登记的幂等键）。
        String key = "kbo-settle-" + s;
        Map<String, Object> settle = new LinkedHashMap<>();
        settle.put("idempotency_key", key);
        settle.put("payment_id", key);
        settle.put("settlement_amount_cent", 10_000);
        settle.put("payment_channel", "cash");
        // 第十八批 O 段第 127 条 127-R3（BSM:940、BSM:1066）：请求里填了别人也不采信，收款人记本次登录结账的授权员工。
        settle.put("cashier_staff_id", "usr_kbo_other_" + s);
        mockMvc.perform(post("/api/v1/bar/bills/" + billId + "/settle").with(kiosk(cashier))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(settle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment_status").value("paid"))
                .andExpect(jsonPath("$.data.settlement_status").value("settled"));
        assertThat(jdbc.queryForMap("SELECT settle_channel, cashier_staff_id, settlement_amount_cent FROM bar_bill_settlements WHERE bill_id = ?", billId))
                .containsEntry("settle_channel", "cash").containsEntry("cashier_staff_id", cashierId)
                .containsEntry("settlement_amount_cent", 10_000L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'payment.cash_received' AND business_id = ?",
                Integer.class, billId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT metadata->>'cashier_staff_id' FROM audit_events WHERE event_name = 'payment.cash_received' AND business_id = ?",
                String.class, billId)).isEqualTo(cashierId);

        // 吧台员工出品：扣吧台库存（既有路径）。
        List<Map<String, Object>> lines = jdbc.queryForList("SELECT id, quantity FROM bar_order_items WHERE order_id = ?", order);
        assertThat(lines).hasSize(1);
        Map<String, Object> deliver = new LinkedHashMap<>();
        deliver.put("idempotency_key", "kbo-deliver-" + s);
        deliver.put("operator_note", "吧台已出酒");
        deliver.put("delivery_photo_attachment_ids", List.of());
        deliver.put("delivered_items", List.of(Map.of("order_item_id", lines.getFirst().get("id"), "quantity", 2)));
        mockMvc.perform(post("/api/v1/bar/orders/" + order + "/deliver").with(barStaff)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(deliver)))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT available_quantity FROM inventory_stock_balances WHERE item_id = ? AND location_id = ?",
                Integer.class, item, location)).isEqualTo(8);
    }

    /** 回归：商户员工在后台网页下单（不带一体机终端与设备号）——来源仍记员工、不记设备；创建终端记服务端认定的可信终端（商户账号的网页请求即商户网页门户）。 */
    @Test
    void backOfficeOrderWithoutKioskStaysStaffSourceWithoutDevice() throws Exception {
        String order = data(order(resident, "web").andExpect(status().isOk())).path("id").asText();
        assertThat(jdbc.queryForMap("SELECT order_source, created_terminal, device_id FROM bar_orders WHERE id = ?", order))
                .containsEntry("order_source", "staff").containsEntry("created_terminal", "merchant_portal").containsEntry("device_id", null);
    }

    /**
     * 第十八批 O 段第 127 条 127-R3（BSM:940「收银、核销、收款、放行等现场动作，一律按执行该动作的设备绑定与该次登录会话定位到操作人并留痕」；
     * BSM:1066「POS（记流水号+收款人）」；BSM:1081「能够明确定位到具体收银员时，由该收银员补足」）：后台网页 POS 结账按网页现形状不带收款人，
     * 收款人记执行结账的登录员工，交班对账把这笔 POS 归入该收银员。
     */
    @Test
    void backOfficeWebPosSettlementRecordsLoggedInCashierAndShiftCloseCountsIt() throws Exception {
        // 交班过合同准入闸（bar.shift_close 越合同业务范围即拦）：本酒吧有一份含酒吧业务的生效合同。
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,"
                + "lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,"
                + "'effective','[\"bar\"]'::jsonb,now(),now())", "kbo_ct_" + s, bar, bar, bar);
        String shift = data(mockMvc.perform(post("/api/v1/bar/shifts/open").with(webCashier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "kbo-shift-" + s, "bar_id", bar,
                                "cashier_user_id", webCashierId, "opening_cash_cent", 0))))
                .andExpect(status().isOk())).path("id").asText();
        String order = data(order(webCashier, "webpos").andExpect(status().isOk())).path("id").asText();
        String billId = data(mockMvc.perform(post("/api/v1/bar/orders/" + order + "/bill").with(webCashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "kbo-webbill-" + s))))
                .andExpect(status().isOk())).path("id").asText();
        String key = "kbo-websettle-" + s;
        mockMvc.perform(post("/api/v1/bar/bills/" + billId + "/settle").with(webCashier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", key, "payment_id", key,
                                "settlement_amount_cent", 10_000, "payment_channel", "pos", "pos_transaction_no", "POS-" + s))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlement_status").value("settled"));
        assertThat(jdbc.queryForObject("SELECT cashier_staff_id FROM bar_bill_settlements WHERE bill_id = ?", String.class, billId))
                .isEqualTo(webCashierId);
        // 第十八批 Q 段第 153 条 153-R1（BSM:1066「POS（记流水号+收款人）」）：契约补声明的 pos_transaction_no 确被服务端读取，落结算流水与收款留痕。
        assertThat(jdbc.queryForObject("SELECT pos_transaction_no FROM bar_bill_settlements WHERE bill_id = ?", String.class, billId))
                .isEqualTo("POS-" + s);
        assertThat(jdbc.queryForObject("SELECT metadata->>'pos_transaction_no' FROM audit_events WHERE event_name = 'payment.pos_received'"
                + " AND business_id = ?", String.class, billId)).isEqualTo("POS-" + s);
        mockMvc.perform(post("/api/v1/bar/shifts/" + shift + "/close").with(webCashier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "kbo-shiftclose-" + s, "declared_cash_cent", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.system_pos_cent").value(10_000));
    }

    /**
     * 第十八批 S 段第 169 条 169-R1（BSM:1066「POS（记流水号+收款人）」；ENUM:107 同句）：后台网页按现形状 POS 结账（蛇形键）不带流水号，
     * 服务端拒绝（400 VALIDATION_FAILED，提示「POS 流水号未填写」），不落结算流水，账单仍未结。
     */
    @Test
    void backOfficeWebPosSettlementWithoutTransactionNoIsRejectedAndBillStaysUnsettled() throws Exception {
        String order = data(order(webCashier, "posnono").andExpect(status().isOk())).path("id").asText();
        String billId = data(mockMvc.perform(post("/api/v1/bar/orders/" + order + "/bill").with(webCashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "kbo-posnonobill-" + s))))
                .andExpect(status().isOk())).path("id").asText();
        String key = "kbo-posnono-" + s;
        mockMvc.perform(post("/api/v1/bar/bills/" + billId + "/settle").with(webCashier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", key, "payment_id", key,
                                "settlement_amount_cent", 10_000, "payment_channel", "pos"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(containsString("POS 流水号")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_bill_settlements WHERE bill_id = ?", Integer.class, billId)).isZero();
        assertThat(jdbc.queryForMap("SELECT payment_status, settlement_status FROM bar_bills WHERE id = ?", billId))
                .containsEntry("payment_status", "unpaid").containsEntry("settlement_status", "unsettled");
    }

    /**
     * 第十八批 P 段第 133 条 133-R1（BSM:1081「收银员开班记备用金，班结按本班次汇总各支付方式应收（现金/扫码/POS/退款/小费），录入实点现金算长短款差异。
     * 收银不应出现差额，出现任何金额的差额一律进复核、定位责任，不设容差」；DEC-R4-2026-08-04-26）：按后台「收银交班对账」区块的真实请求形状
     * （收银员取登录人、不带容差，蛇形键）开班，网页现金结账（同网页现形状，金额留空按剩余应收结清），班结差 1 分也进复核；再开一班实点正好不进复核。
     * 第十八批 Q 段第 141 条 141-R1：开班不再收容差字段，班次返回体也没有 cash_diff_threshold_cent。
     */
    @Test
    void backOfficeShiftPanelOpensWithoutThresholdAndAnyCashDifferenceNeedsReview() throws Exception {
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,"
                + "lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,"
                + "'effective','[\"bar\"]'::jsonb,now(),now())", "kbo_ct_" + s, bar, bar, bar);
        String first = openFromPanel("kbo-panel-open1-" + s);
        String order = data(order(webCashier, "panelcash").andExpect(status().isOk())).path("id").asText();
        String billId = data(mockMvc.perform(post("/api/v1/bar/orders/" + order + "/bill").with(webCashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "kbo-panelbill-" + s))))
                .andExpect(status().isOk())).path("id").asText();
        String key = "kbo-panelsettle-" + s;
        mockMvc.perform(post("/api/v1/bar/bills/" + billId + "/settle").with(webCashier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", key, "payment_id", key,
                                "payment_channel", "cash"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlement_status").value("settled"));
        closeFromPanel(first, "kbo-panel-close1-" + s, 29_999)
                .andExpect(jsonPath("$.data.system_cash_cent").value(10_000))
                .andExpect(jsonPath("$.data.cash_diff_cent").value(-1))
                .andExpect(jsonPath("$.data.needs_review").value(true));

        String second = openFromPanel("kbo-panel-open2-" + s);
        closeFromPanel(second, "kbo-panel-close2-" + s, 20_000)
                .andExpect(jsonPath("$.data.cash_diff_cent").value(0))
                .andExpect(jsonPath("$.data.needs_review").value(false));
    }

    /**
     * 第十八批 S 段第 171 条 171-R1（BSM:1066「储值卡（通用卡先扣本金后扣赠送金且不可配置……）」；BSM:1563「验证码……（用于新人判定、注册登录和关键操作验证）」）：
     * 契约补声明的 POST /api/v1/bar/bills/{bill_id}/stored-value/sms 按网页收银员真实请求（蛇形键 phone）走真实入口与真实授权链——
     * phone 被读取并按会员核对（非会员 401 CUSTOMER_OTP_INVALID），缺 phone 400 VALIDATION_FAILED，已结账 409 BAR_BILL_ALREADY_SETTLED。
     */
    @Test
    void webCashierStoredValueSmsReadsPhoneAndRejectsMissingPhoneAndSettledBill() throws Exception {
        String order = data(order(webCashier, "svsms").andExpect(status().isOk())).path("id").asText();
        String billId = data(mockMvc.perform(post("/api/v1/bar/orders/" + order + "/bill").with(webCashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "kbo-svbill-" + s))))
                .andExpect(status().isOk())).path("id").asText();
        String sms = "/api/v1/bar/bills/" + billId + "/stored-value/sms";
        String stranger = "198" + String.format("%08d", Math.floorMod(s.hashCode(), 100_000_000));
        mockMvc.perform(post(sms).with(webCashier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", stranger))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_OTP_INVALID"));
        mockMvc.perform(post(sms).with(webCashier).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));

        String key = "kbo-svsettle-" + s;
        mockMvc.perform(post("/api/v1/bar/bills/" + billId + "/settle").with(webCashier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", key, "payment_id", key,
                                "settlement_amount_cent", 10_000, "payment_channel", "cash"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlement_status").value("settled"));
        mockMvc.perform(post(sms).with(webCashier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("phone", stranger))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_BILL_ALREADY_SETTLED"));
    }

    // ---------- helpers ----------

    /** 后台「收银交班对账」区块的开班请求：收银员取登录人；开班不设容差（141-R1），返回体没有 cash_diff_threshold_cent。 */
    private String openFromPanel(String key) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("bar_id", bar);
        body.put("cashier_user_id", webCashierId);
        body.put("opening_cash_cent", 20_000);
        body.put("note", "早班");
        return data(mockMvc.perform(post("/api/v1/bar/shifts/open").with(webCashier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("open"))
                .andExpect(jsonPath("$.data.cash_diff_threshold_cent").doesNotExist())).path("id").asText();
    }

    /** 后台「收银交班对账」区块的班结请求：只带实点现金（接班人、备注选填未填）。 */
    private ResultActions closeFromPanel(String shift, String key, long declaredCashCent) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/shifts/" + shift + "/close").with(webCashier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", key, "declared_cash_cent", declaredCashCent))))
                .andExpect(status().isOk());
    }

    /** 一体机真实形状：一体机终端头＋登记设备号（不签名，默认 grace）。 */
    private RequestPostProcessor kiosk(RequestPostProcessor who) {
        return request -> {
            MockHttpServletRequest r = who.postProcessRequest(request);
            r.removeHeader("X-Terminal-Type");
            r.addHeader("X-Terminal-Type", "kiosk");
            r.addHeader("X-Device-Id", device);
            return r;
        };
    }

    /** 一体机吧台买酒屏的真实请求形状：本机酒吧吧台位，按商品与数量下单。 */
    private ResultActions order(RequestPostProcessor who, String tag) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "kbo-order-" + tag + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", table);
        body.put("items", List.of(Map.of("product_id", product, "quantity", "2")));
        return mockMvc.perform(post("/api/v1/bar/orders").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private JsonNode data(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
    }
}
