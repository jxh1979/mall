package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AA 段第 206 条 206-R1 台位停用与恢复（真库）。依据：ENUM:100「桌台/卡座/吧台位状态｜空闲、已预留、已开台、待清台、停用、异常。……
 * 已预留只能经取消订台、超时释放、到店开台三条路径退出……台位停用与恢复由店长及以上操作，填原因留痕」；BSM:1058 同句。
 * 后台请求按前端真实形状（蛇形键），走真实入口与真实授权链（access_users＋角色→权限码）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarTableDisableJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "btd_prj_" + s;
    private final String bar = "btd_bar_" + s;
    private final String product = "btd_product_" + s;
    private final String customerToken = "tok_btd_" + s;

    private StaffRequestFixture staffs;
    private RequestPostProcessor manager;
    private String seat;
    private String seat2;
    private String counter;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','台位停用综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'台位停用酒吧','bar','active',?,now(),now())",
                bar, project);
        seat = table(bar, "seat", "card_seat");
        seat2 = table(bar, "seat2", "card_seat");
        counter = table(bar, "counter", "bar_counter");
        jdbc.update("INSERT INTO bar_products (id, bar_id, merchant_id, product_name, product_type, price_cent, currency,"
                + " stock_deduction_required, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, '精酿啤酒', 'beer', 3800, 'CNY', FALSE, 'active', now(), now())", product, bar, bar);
        staffs = new StaffRequestFixture(jdbc);
        // 店长：停用与恢复、开台、转台、员工代订、下单。
        manager = staffs.staff("usr_btd_manager_" + s, "staff", project, bar, "bar.table.disable", "bar.table_session.create",
                "bar.table_session.transfer", "bar.reservation.create", "bar.order.create");
        Instant now = Instant.now();
        String customerId = "cus_btd_" + s;
        String phone = "135" + String.format("%08d", Math.floorMod(s.hashCode(), 100_000_000));
        customers.saveCustomer(new CustomerRecord(customerId, phone, "135****" + phone.substring(7), "active", null, null, now, now));
        customers.saveSession(new CustomerSessionRecord("sess_btd_" + s, customerId, customerToken, "miniapp", now,
                now.plusSeconds(3600), "active"));
    }

    @Test
    void disableIdleTableRecordsReasonAndBlocksOpenTransferBookingAndCustomerOptionsUntilEnabled() throws Exception {
        options().andExpect(jsonPath(option(seat)).exists());
        change(manager, seat, "disable", "dis", "空调漏水维修")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("disabled"));
        assertThat(tableStatus(seat)).isEqualTo("disabled");
        assertThat(audit("bar.table.disabled", seat))
                .containsEntry("reason", "空调漏水维修").containsEntry("status_before", "active").containsEntry("status_after", "disabled");

        // 停用台不能开台、不能作为转台目标、不能被员工代订，客人订台选项里也不再出现。
        openSession("open-disabled", seat)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_NOT_OPENABLE"));
        String session = data(openSession("open-seat2", seat2).andExpect(status().isOk())).path("id").asText();
        transfer(session, seat)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_NOT_TRANSFERABLE"));
        staffReserve("book-disabled", seat)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_ALREADY_RESERVED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_reservations WHERE table_id = ? AND hold_status = 'held'", Integer.class, seat)).isZero();
        options().andExpect(jsonPath(option(seat)).doesNotExist());

        // 恢复：回到空闲、留痕带原因与前后状态，之后可正常开台。
        change(manager, seat, "enable", "en", "维修完毕")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("active"));
        assertThat(audit("bar.table.enabled", seat))
                .containsEntry("reason", "维修完毕").containsEntry("status_before", "disabled").containsEntry("status_after", "active");
        openSession("open-enabled", seat).andExpect(status().isOk());
        assertThat(tableStatus(seat)).isEqualTo("opened");
    }

    @Test
    void disabledBarCounterRejectsOrderUntilEnabled() throws Exception {
        change(manager, counter, "disable", "dis-counter", "吧台位设备故障").andExpect(status().isOk());
        order("disabled", counter)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_DISABLED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_orders WHERE table_id = ?", Integer.class, counter)).isZero();
        change(manager, counter, "enable", "en-counter", "已修好").andExpect(status().isOk());
        order("enabled", counter).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_orders WHERE table_id = ?", Integer.class, counter)).isEqualTo(1);
    }

    @Test
    void onlyIdleTableCanBeDisabledAndOnlyDisabledTableCanBeEnabled() throws Exception {
        change(manager, seat, "disable", "no-reason", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(tableStatus(seat)).isEqualTo("active");
        change(manager, seat, "enable", "en-idle", "误点恢复")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_NOT_DISABLED"));

        // 已开台、已预留、待清台都不能直接停用（须先按关台、取消订台、清台流程回到空闲）。
        openSession("opened", seat).andExpect(status().isOk());
        change(manager, seat, "disable", "dis-opened", "要停用")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_NOT_DISABLABLE"));
        staffReserve("held", seat2).andExpect(status().isOk()).andExpect(jsonPath("$.data.hold_status").value("held"));
        change(manager, seat2, "disable", "dis-held", "要停用")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_NOT_DISABLABLE"));
        jdbc.update("UPDATE bar_tables SET status = 'pending_clear' WHERE id = ?", counter);
        change(manager, counter, "disable", "dis-pending", "要停用")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_NOT_DISABLABLE"));
        assertThat(List.of(tableStatus(seat), tableStatus(seat2), tableStatus(counter)))
                .containsExactly("opened", "active", "pending_clear");
    }

    @Test
    void disableRequiresPermissionAndOwnMerchantWithoutConsumingIdempotencyKey() throws Exception {
        RequestPostProcessor clerk = staffs.staff("usr_btd_clerk_" + s, "staff", project, bar, "bar.table.open_close");
        change(clerk, seat, "disable", "clerk", "店员想停用").andExpect(status().isForbidden());
        assertThat(tableStatus(seat)).isEqualTo("active");

        String otherBar = "btd_bar2_" + s;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'隔壁酒吧','bar','active',?,now(),now())",
                otherBar, project);
        String otherTable = table(otherBar, "other", "card_seat");
        change(manager, otherTable, "disable", "cross", "停别家的台")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(tableStatus(otherTable)).isEqualTo("active");
        // 商户闸在幂等之前：被拒的那次不占请求编号，本商户店长用同一编号照常办理。
        RequestPostProcessor otherManager = staffs.staff("usr_btd_manager2_" + s, "staff", project, otherBar, "bar.table.disable");
        change(otherManager, otherTable, "disable", "cross", "本店停用")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("disabled"));
    }

    // ---------- helpers ----------

    private String table(String barId, String tag, String type) {
        String id = "btd_table_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, '台位停用回归', ?, 6, 2, 0, 'CNY', 'active', now(), now())",
                id, barId, barId, "T-" + tag.toUpperCase() + "-" + s, type);
        return id;
    }

    /** 后台卡座表「停用」「恢复」按钮的请求形状：原因必填。 */
    private ResultActions change(RequestPostProcessor who, String tableId, String action, String tag, String reason) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "btd-" + tag + "-" + s);
        if (reason != null) {
            body.put("reason", reason);
        }
        return mockMvc.perform(post("/api/v1/bar/tables/" + tableId + "/" + action).with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 后台开台表单（散客开台，不带订台号）。 */
    private ResultActions openSession(String tag, String tableId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "btd-" + tag + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", tableId);
        body.put("business_date", java.time.LocalDate.now().toString());
        body.put("guest_count", 2);
        return mockMvc.perform(post("/api/v1/bar/table-sessions").with(manager)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions transfer(String sessionId, String targetTableId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "btd-transfer-" + s);
        body.put("target_table_id", targetTableId);
        body.put("reason", "客人想换台");
        body.put("session_version", 1);
        return mockMvc.perform(post("/api/v1/bar/table-sessions/" + sessionId + "/transfer").with(manager)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 后台代订（新建订台表单的真实请求形状）。 */
    private ResultActions staffReserve(String tag, String tableId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "btd-" + tag + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", tableId);
        body.put("guest_count", 2);
        body.put("reservation_time", Instant.now().plusSeconds(3600).toString());
        body.put("latest_arrival_at", Instant.now().plusSeconds(7200).toString());
        body.put("customer_phone", "13900001234");
        return mockMvc.perform(post("/api/v1/bar/reservations").with(manager)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 吧台位不经开台直接下单（后台点单与一体机吧台买酒同一入口）。 */
    private ResultActions order(String tag, String tableId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "btd-order-" + tag + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", tableId);
        body.put("items", List.of(Map.of("product_id", product, "quantity", 1)));
        return mockMvc.perform(post("/api/v1/bar/orders").with(manager)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions options() throws Exception {
        return mockMvc.perform(get("/api/v1/customer/bar-reservation-options").header("X-Terminal-Type", "miniapp")
                .header("X-Customer-Token", customerToken)).andExpect(status().isOk());
    }

    private static String option(String tableId) {
        return "$.data.items[?(@.table_id == '" + tableId + "')]";
    }

    private String tableStatus(String tableId) {
        return jdbc.queryForObject("SELECT status FROM bar_tables WHERE id = ?", String.class, tableId);
    }

    private Map<String, Object> audit(String event, String tableId) {
        return jdbc.queryForMap("SELECT metadata->>'reason' AS reason, metadata->>'status_before' AS status_before,"
                + " metadata->>'status_after' AS status_after FROM audit_events WHERE event_name = ? AND business_id = ?", event, tableId);
    }

    private JsonNode data(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
    }
}
