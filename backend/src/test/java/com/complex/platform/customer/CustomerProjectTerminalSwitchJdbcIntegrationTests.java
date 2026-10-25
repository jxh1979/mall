package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * PGM 第 736 行「可以按全局、综合体、商户、业务系统、模块、版本/设备、灰度开关」、第 744 行「前端隐藏只是辅助，后端接口必须独立校验」：
 * 票种列表与订台选项每项带所属综合体 project_id，客人端购票、订台按所选票种／台位的 project_id 带 X-Project-Id——
 * 有两个综合体时关掉其一（终端开关按综合体），该综合体的购票、订台被后端拒绝，另一个照常；综合体由服务端按票种／台位所属酒吧自己取，
 * 不带 X-Project-Id 或把头写成另一个综合体都绕不过。
 * 真库、真接口，客人请求按客人端真实形状（蛇形键、X-Terminal-Type、X-Customer-Token）；app、miniapp 翻成 enforce（与生产一致）。
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
class CustomerProjectTerminalSwitchJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;

    /** 综合体甲（被关的那个）与综合体乙：各自一个已发布票种；客人用甲的会话买两边的票、订两边的台。 */
    private CustomerTicketPaymentFixture closed;
    private CustomerTicketPaymentFixture open;

    @BeforeEach
    void seed() {
        closed = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        open = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        jdbc.update("""
                INSERT INTO terminal_capability_switches (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
                VALUES (?, 'miniapp', 'terminal.enabled', 'project', ?, FALSE, '停用该综合体客人端', now(), now())""",
                "tcs_b15_proj_" + closed.suffix, closed.projectId);
    }

    @Test
    void ticketProductsCarryProjectAndClosedProjectPurchaseRejected() throws Exception {
        JsonNode products = items(get("/api/v1/customer/ticket-products"));
        String closedProject = field(products, "ticket_type_id", closed.ticketTypeId, "project_id");
        String openProject = field(products, "ticket_type_id", open.ticketTypeId, "project_id");
        assertThat(closedProject).isEqualTo(closed.projectId);
        assertThat(openProject).isEqualTo(open.projectId);

        order(closed.ticketTypeId, closedProject)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_orders WHERE ticket_type_id = ?", Integer.class,
                closed.ticketTypeId)).isZero();
        order(open.ticketTypeId, openProject)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project_id").value(open.projectId));
    }

    @Test
    void reservationOptionsCarryProjectAndClosedProjectReservationRejected() throws Exception {
        String closedTable = bar(closed);
        String openTable = bar(open);
        JsonNode options = items(get("/api/v1/customer/bar-reservation-options"));
        String closedProject = field(options, "table_id", closedTable, "project_id");
        String openProject = field(options, "table_id", openTable, "project_id");
        assertThat(closedProject).isEqualTo(closed.projectId);
        assertThat(openProject).isEqualTo(open.projectId);

        reserve(closed, closedTable, closedProject)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_reservations WHERE table_id = ?", Integer.class, closedTable)).isZero();
        reserve(open, openTable, openProject).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_reservations WHERE table_id = ?", Integer.class, openTable)).isEqualTo(1);
    }

    /** PGM 第 744 行：综合体由服务端按票种自己取——不带 X-Project-Id、或把头写成另一个（开着的）综合体，被关综合体的票照样买不了。 */
    @Test
    void closedProjectPurchaseRejectedWithoutOrWithForgedProjectHeader() throws Exception {
        for (String header : new String[] {null, open.projectId}) {
            order(closed.ticketTypeId, header)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_orders WHERE ticket_type_id = ?", Integer.class,
                closed.ticketTypeId)).isZero();
    }

    /** PGM 第 744 行：综合体由服务端按台位所属酒吧自己取——不带 X-Project-Id、或把头写成另一个综合体，被关综合体的台照样订不了。 */
    @Test
    void closedProjectReservationRejectedWithoutOrWithForgedProjectHeader() throws Exception {
        String closedTable = bar(closed);
        for (String header : new String[] {null, open.projectId}) {
            reserve(closed, closedTable, header)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_reservations WHERE table_id = ?", Integer.class, closedTable)).isZero();
    }

    // ---------- helpers ----------

    /** 该综合体的一家酒吧、一个卡座与固定定金规则（客人自助订台必须支付定金，BSM 第 1062 行）；返回台位编号。 */
    private String bar(CustomerTicketPaymentFixture project) {
        String barId = "bar_b15proj_" + project.suffix;
        String tableId = "table_b15proj_" + project.suffix;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '综合体开关测试酒吧', 'bar', 'active', now(), now(), ?)", barId, project.projectId);
        jdbc.update("INSERT INTO bar_tables (id, bar_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, '卡座', 'card_seat', 6, 2, 50000, 'CNY', 'active', now(), now())",
                tableId, barId, "T-B15-" + project.suffix);
        jdbc.update("INSERT INTO bar_deposit_rules (id, bar_id, scope_type, scope_id, mode, fixed_amount_cent, cancel_free_before_minutes,"
                + " enabled, created_at, updated_at) VALUES (?, ?, 'bar', ?, 'fixed', 20000, 0, TRUE, now(), now())",
                "bar_dep_b15proj_" + project.suffix, barId, barId);
        return tableId;
    }

    private JsonNode items(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
        String body = mockMvc.perform(request.header("X-Terminal-Type", "miniapp").header("X-Customer-Token", closed.token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return JSON.readTree(body).path("data").path("items");
    }

    private static String field(JsonNode items, String key, String value, String wanted) {
        for (JsonNode item : items) {
            if (value.equals(item.path(key).asText())) {
                return item.path(wanted).isNull() ? null : item.path(wanted).asText(null);
            }
        }
        throw new AssertionError("列表里没有 " + key + "=" + value);
    }

    /** 客人端购票真实请求形状：蛇形键＋所选票种的综合体放 X-Project-Id。 */
    private ResultActions order(String ticketTypeId, String projectId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        String key = "b15proj-order-" + UUID.randomUUID();
        body.put("idempotency_key", key);
        body.put("ticket_type_id", ticketTypeId);
        body.put("quantity", 1);
        body.put("visit_date", LocalDate.now().toString());
        body.put("contact_phone", closed.phone);
        return send("/api/v1/customer/ticket-orders", key, projectId, body);
    }

    /** 客人端订台真实请求形状：蛇形键＋所选台位的综合体放 X-Project-Id。 */
    private ResultActions reserve(CustomerTicketPaymentFixture project, String tableId, String projectId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        String key = "b15proj-reserve-" + UUID.randomUUID();
        body.put("idempotency_key", key);
        body.put("bar_id", "bar_b15proj_" + project.suffix);
        body.put("table_id", tableId);
        body.put("table_type", "card_seat");
        body.put("reservation_time", Instant.now().plusSeconds(3600).toString());
        body.put("latest_arrival_at", Instant.now().plusSeconds(7200).toString());
        body.put("guest_count", 2);
        return send("/api/v1/customer/bar-reservations", key, projectId, body);
    }

    private ResultActions send(String path, String key, String projectId, Map<String, Object> body) throws Exception {
        var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", closed.token).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body));
        if (projectId != null) {
            request.header("X-Project-Id", projectId);
        }
        return mockMvc.perform(request);
    }
}
