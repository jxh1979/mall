package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.TemporaryGrant;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
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
 * 第十八批 T 段第 157 条 157-R7（真库）：BSM:1113「不设额度时，风险看板和审计导出必须持续展示挂起金额、账龄、责任人和未结清数量。」；
 * BSM:1840「看板按权限看，但把数据导出带走须单独授权并留痕」。两家酒吧各挂包厢单（经真实挂起入口），酒吧 A 的单含坏账、取消审批中与已结清各一笔；
 * 看板与导出按后台订单页真实形状（蛇形查询参数、X-Terminal-Type: web）经控制器与真实授权链请求。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarSuspendedOrderBoardJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BarRepository bars;
    @Autowired private AccessRepository access;

    private String s;
    private String barA;
    private String barB;
    private String wineA;
    private String wineB;
    private String cashierAId;
    private String waiterId;
    private RequestPostProcessor cashierA;
    private RequestPostProcessor cashierB;
    private String hanging;
    private String storeLoss;
    private String badDebt;
    private String cancelPending;
    private String settled;
    private String otherBar;

    @BeforeEach
    void seed() throws Exception {
        s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String project = "prj_board_" + s;
        String company = "ent_board_" + s;
        barA = "bar_board_a_" + s;
        barB = "bar_board_b_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','挂起看板综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'看板酒吧甲','bar',?,?,?,?,'active',now(),now()),(?,'看板酒吧乙','bar',?,?,?,?,'active',now(),now())",
                barA, company, company, company, project, barB, company, company, company, project);
        for (String bar : List.of(barA, barB)) {
            jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) VALUES(?,?,?,'包厢区','unit','active',now(),now())",
                    "space_" + bar, project, bar);
        }
        // 包厢开账单要有该营业日已批准的收费规则（服务费率取 0，账单金额即消费额）。
        jdbc.update("""
                INSERT INTO bar_payroll_rules(id, rule_kind, project_id, merchant_id, effective_from, version, rule_json, reason, status,
                    platform_company_id, created_by)
                VALUES (?, 'charge', ?, NULL, ?, 'V0.01', ?::jsonb, '服务费 0', 'approved', ?, 'usr_board')
                """, "bpr_board_" + s, project, java.sql.Date.valueOf(LocalDate.now().minusDays(1)),
                json.writeValueAsString(new BarPayrollRequests.RuleValues(0, 0, 2000, null, null, null, List.of())), company);
        Instant now = Instant.now();
        wineA = "product_board_a_" + s;
        wineB = "product_board_b_" + s;
        bars.saveProduct(new BarProductRecord(wineA, barA, barA, "干红", "wine", 30000L, "CNY", null, null, false, "active",
                now, now, null, null, null, false, null, null, null));
        bars.saveProduct(new BarProductRecord(wineB, barB, barB, "干红", "wine", 30000L, "CNY", null, null, false, "active",
                now, now, null, null, null, false, null, null, null));
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        cashierAId = "usr_board_cashier_a_" + s;
        waiterId = "usr_board_waiter_" + s;
        cashierA = staffs.staff(cashierAId, "staff", project, barA, "bar.order.create", "bar.order.view", "bar.bill.hang",
                "finance.payment.register", "bar.table_session.transfer");
        cashierB = staffs.staff("usr_board_cashier_b_" + s, "staff", project, barB, "bar.order.create", "bar.order.view", "bar.bill.hang");
        staffs.staff(waiterId, "staff", project, barA, "bar.order.view");

        hanging = suspended(cashierA, barA, wineA, "a1", true);
        storeLoss = suspended(cashierA, barA, wineA, "a2", false);
        badDebt = suspended(cashierA, barA, wineA, "a3", false);
        cancelPending = suspended(cashierA, barA, wineA, "a4", false);
        settled = suspended(cashierA, barA, wineA, "a5", false);
        otherBar = suspended(cashierB, barB, wineB, "b1", false);
        // 已挂起 90 分钟仍未付；坏账、取消审批中两笔直接置状态（看板只读状态，不经各自入口）。
        jdbc.update("UPDATE bar_order_suspensions SET suspended_at = now() - interval '90 minutes' WHERE order_id = ?", hanging);
        jdbc.update("UPDATE bar_order_suspensions SET status = 'bad_debt_processing', updated_at = now() WHERE order_id = ?", badDebt);
        jdbc.update("UPDATE bar_order_suspensions SET status = 'cancel_pending_approval', updated_at = now() WHERE order_id = ?", cancelPending);
        String settledBill = jdbc.queryForObject("SELECT bill_id FROM bar_order_suspensions WHERE order_id = ?", String.class, settled);
        settle(settledBill).andExpect(status().isOk());
        // 结清前已挂起 30 分钟：账龄止于结清时点。
        jdbc.update("UPDATE bar_order_suspensions SET suspended_at = updated_at - interval '30 minutes' WHERE order_id = ?", settled);
    }

    /**
     * 酒吧 A 商户账号只见本酒吧；汇总按未结清（已挂起待付款、取消审批中、坏账损耗处理中）计数与未收金额，不含已结清；账龄未结清算到此刻、
     * 已结清止于结清时点；责任人取本单已出品明细出品时刻的主责服务员账号，取不到的为门店损耗（BSM:1110）；按状态筛选只筛列表、汇总不变；读看板留痕。
     */
    @Test
    void merchantSeesOwnBarWithUnsettledSummaryAgeAndResponsible() throws Exception {
        JsonNode board = data(mockMvc.perform(get("/api/v1/bar/suspended-orders").param("page", "1").param("size", "50").with(cashierA))
                .andExpect(status().isOk()));
        Map<String, JsonNode> rows = rows(board);
        assertThat(rows.keySet()).containsExactlyInAnyOrder(hanging, storeLoss, badDebt, cancelPending, settled);
        assertThat(board.at("/summary/unsettled_count").asLong()).isEqualTo(4);
        assertThat(board.at("/summary/unsettled_amount_cent").asLong()).isEqualTo(120000);
        assertThat(board.at("/summary/oldest_unsettled_age_minutes").asLong()).isBetween(90L, 91L);
        assertThat(board.at("/total").asLong()).isEqualTo(5);

        JsonNode first = rows.get(hanging);
        assertThat(first.at("/status").asText()).isEqualTo("hanging_unpaid");
        assertThat(first.at("/amount_cent").asLong()).isEqualTo(30000);
        assertThat(first.at("/unpaid_cent").asLong()).isEqualTo(30000);
        assertThat(first.at("/age_minutes").asLong()).isBetween(90L, 91L);
        assertThat(first.at("/responsible_users").size()).isEqualTo(1);
        assertThat(first.at("/responsible_users/0/user_id").asText()).isEqualTo(waiterId);
        assertThat(first.at("/responsible_users/0/username").asText()).isEqualTo(waiterId);
        assertThat(first.at("/suspended_by").asText()).isEqualTo(cashierAId);
        assertThat(first.at("/bar_name").asText()).isEqualTo("看板酒吧甲");
        assertThat(first.at("/table_name").asText()).isEqualTo("包厢 a1");
        assertThat(first.at("/bill_no").asText()).isNotBlank();
        assertThat(rows.get(storeLoss).at("/responsible_users").size()).isEqualTo(1);
        assertThat(rows.get(storeLoss).at("/responsible_users/0/user_id").isNull()).isTrue();
        assertThat(rows.get(settled).at("/status").asText()).isEqualTo("settled");
        assertThat(rows.get(settled).at("/unpaid_cent").asLong()).isZero();
        assertThat(rows.get(settled).at("/age_minutes").asLong()).isEqualTo(30);
        assertThat(rows.get(badDebt).at("/status").asText()).isEqualTo("bad_debt_processing");

        JsonNode onlySettled = data(mockMvc.perform(get("/api/v1/bar/suspended-orders").param("status", "settled").with(cashierA))
                .andExpect(status().isOk()));
        assertThat(rows(onlySettled).keySet()).containsExactly(settled);
        assertThat(onlySettled.at("/summary/unsettled_count").asLong()).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'bar.order_suspension.viewed' AND actor_id = ?",
                Integer.class, cashierAId)).isEqualTo(2);

        JsonNode other = data(mockMvc.perform(get("/api/v1/bar/suspended-orders").with(cashierB)).andExpect(status().isOk()));
        assertThat(rows(other).keySet()).containsExactly(otherBar);
        assertThat(other.at("/summary/unsettled_count").asLong()).isEqualTo(1);
    }

    /** 导出须单独授权：没有单次授权 403；授予后导出带密码 PDF、落导出记录与审计，内容含未结清数量、中文状态与门店损耗；用掉后再导 403。 */
    @Test
    void exportNeedsSingleUseGrantAndLeavesRecord() throws Exception {
        String deviceNo = "WBBOARD" + s.toUpperCase();
        jdbc.update("INSERT INTO browser_devices (device_no, first_user_id) VALUES (?, ?)", deviceNo, cashierAId);
        Cookie device = new Cookie("complex_device_no", deviceNo);

        mockMvc.perform(get("/api/v1/bar/suspended-orders/export").with(cashierA).cookie(device)).andExpect(status().isForbidden());

        Instant now = Instant.now();
        access.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), cashierAId, List.of("audit.operation_log.export"),
                now.minusSeconds(3600), now.plusSeconds(86_400), true, false, "active", "挂起看板导出回归单次授权", "seed-admin", now, null, null));
        byte[] pdf = mockMvc.perform(get("/api/v1/bar/suspended-orders/export").with(cashierA).cookie(device))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        Map<String, Object> file = jdbc.queryForMap("""
                SELECT id, row_count, filters->>'merchant_id' AS merchant_id FROM export_files
                 WHERE exporter_id = ? AND export_type = 'bar_suspended_orders'""", cashierAId);
        assertThat(file.get("row_count")).isEqualTo(5);
        assertThat(file.get("merchant_id")).isEqualTo(barA);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'bar.order_suspension.exported'"
                + " AND event_result = 'success' AND business_id = ?", Integer.class, file.get("id"))).isEqualTo(1);
        String password = data(mockMvc.perform(get("/api/v1/security/my-exports").with(cashierA)).andExpect(status().isOk()))
                .at("/items/0/password").asText();
        try (var doc = org.apache.pdfbox.Loader.loadPDF(pdf, password)) {
            assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(doc))
                    .contains("挂起订单风险看板", "未结清 4 单", "已挂起待付款", "坏账损耗处理中", "门店损耗", "账龄");
        }

        mockMvc.perform(get("/api/v1/bar/suspended-orders/export").with(cashierA).cookie(device)).andExpect(status().isForbidden());
    }

    /**
     * 18T 复核修复④（BSM:1110「服务员责任以商品出品时该桌绑定的主责服务员为准，后续换台、换班或改绑不追溯变更；出品时没有主责服务员的记为门店损耗」；
     * BSM:1113 风险看板和审计导出展示责任人）：包厢开台主责服务员甲、出品后在卡座页服务交接给乙、再由收银员挂起——看板与导出的责任人都是出品时的甲，不是乙。
     */
    @Test
    void responsibleIsWaiterAtDeliveryNotHandoverSuccessor() throws Exception {
        String nextId = "usr_board_next_" + s;
        new StaffRequestFixture(jdbc).staff(nextId, "staff", "prj_board_" + s, barA, "bar.order.create");
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,company_id,registration_status,status,employment_status,"
                + "is_sales_position,created_at,updated_at) VALUES(?,?,?,?,'接手服务员',?,'approved','active','employed',FALSE,now(),now())",
                "pro_board_" + s, "prj_board_" + s, barA, nextId, "ent_board_" + s);
        jdbc.update("UPDATE access_users SET username = '服务员甲' WHERE id = ?", waiterId);
        jdbc.update("UPDATE access_users SET username = '服务员乙' WHERE id = ?", nextId);
        String order = suspended(cashierA, barA, wineA, "h1", true, nextId);

        String deviceNo = "WBHAND" + s.toUpperCase();
        jdbc.update("INSERT INTO browser_devices (device_no, first_user_id) VALUES (?, ?)", deviceNo, cashierAId);
        Instant now = Instant.now();
        access.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), cashierAId, List.of("audit.operation_log.export"),
                now.minusSeconds(3600), now.plusSeconds(86_400), true, false, "active", "挂起看板交接回归单次授权", "seed-admin", now, null, null));
        byte[] pdf = mockMvc.perform(get("/api/v1/bar/suspended-orders/export").with(cashierA).cookie(new Cookie("complex_device_no", deviceNo)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        String password = data(mockMvc.perform(get("/api/v1/security/my-exports").with(cashierA)).andExpect(status().isOk()))
                .at("/items/0/password").asText();
        try (var doc = org.apache.pdfbox.Loader.loadPDF(pdf, password)) {
            assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(doc)).contains("服务员甲").doesNotContain("服务员乙");
        }

        JsonNode row = rows(data(mockMvc.perform(get("/api/v1/bar/suspended-orders").with(cashierA)).andExpect(status().isOk()))).get(order);
        assertThat(row.at("/responsible_users").size()).isEqualTo(1);
        assertThat(row.at("/responsible_users/0/user_id").asText()).isEqualTo(waiterId);
        assertThat(row.at("/responsible_users/0/username").asText()).isEqualTo("服务员甲");
    }

    // ---------- helpers ----------

    private String suspended(RequestPostProcessor who, String bar, String wine, String tag, boolean withWaiter) throws Exception {
        return suspended(who, bar, wine, tag, withWaiter, null);
    }

    /**
     * 在该酒吧开一张包厢、开台、下一瓶干红、生成账单、出品并经真实入口挂起；withWaiter 时开台会话有主责服务员；handoverTo 非空时出品后先经卡座页
     * 「服务交接」交给此人再挂起。返回订单号。
     */
    private String suspended(RequestPostProcessor who, String bar, String wine, String tag, boolean withWaiter, String handoverTo)
            throws Exception {
        Instant now = Instant.now();
        String table = "table_board_" + tag + "_" + s;
        bars.saveTable(new BarTableRecord(table, bar, bar, "T-" + tag + "-" + s, "包厢 " + tag, "vip", 4, 0, 0L, "CNY", "opened", now, now));
        bars.assignTableSpace(table, "space_" + bar);
        String session = "session_board_" + tag + "_" + s;
        bars.saveTableSession(new BarTableSessionRecord(session, bar, bar, LocalDate.now(), table, null, null, 2,
                0L, 0, null, withWaiter ? waiterId : null, "walk_in", "open", now, null, null, 1L, now, now));
        if (withWaiter) {
            jdbc.update("INSERT INTO bar_service_responsibilities(id,session_id,table_id,project_id,merchant_id,user_id,payer_company_id,"
                    + "started_at,source_id,recorded_by,reason) VALUES(?,?,?,?,?,?,?,now() - interval '2 hours',?,?,'开台确认责任人')",
                    "bsr_board_" + tag + "_" + s, session, table, "prj_board_" + s, bar, waiterId, "ent_board_" + s, "open:" + session, cashierAId);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "board-order-" + tag + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", table);
        body.put("items", List.of(Map.of("product_id", wine, "quantity", 1)));
        String order = read(mockMvc.perform(post("/api/v1/bar/orders").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isOk()), "$.data.id");
        mockMvc.perform(post("/api/v1/bar/orders/" + order + "/bill").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "board-bill-" + tag + "-" + s)))).andExpect(status().isOk());
        List<Map<String, Object>> lines = jdbc.queryForList("SELECT id, quantity FROM bar_order_items WHERE order_id = ?", order).stream()
                .map(row -> Map.<String, Object>of("order_item_id", row.get("id"), "quantity", row.get("quantity"))).toList();
        Map<String, Object> deliver = new LinkedHashMap<>();
        deliver.put("idempotency_key", "board-deliver-" + tag + "-" + s);
        deliver.put("operator_note", "已送达");
        deliver.put("delivery_photo_attachment_ids", List.of());
        deliver.put("delivered_items", lines);
        mockMvc.perform(post("/api/v1/bar/orders/" + order + "/deliver").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(deliver))).andExpect(status().isOk());
        if (handoverTo != null) {
            Map<String, Object> handover = new LinkedHashMap<>();
            handover.put("idempotency_key", "board-handover-" + tag + "-" + s);
            handover.put("next_user_id", handoverTo);
            handover.put("session_version", jdbc.queryForObject("SELECT session_version FROM bar_table_sessions WHERE id = ?", Long.class, session));
            handover.put("reason", "换班交接");
            mockMvc.perform(post("/api/v1/bar/table-sessions/" + session + "/service-handover").with(who).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(handover))).andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/bar/orders/" + order + "/suspend").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "board-hang-" + tag + "-" + s, "reason", "客人稍后付款"))))
                .andExpect(status().isOk());
        return order;
    }

    private ResultActions settle(String bill) throws Exception {
        String key = "board-settle-" + s;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("payment_id", key);
        body.put("settlement_amount_cent", 30000L);
        body.put("payment_channel", "cash");
        return mockMvc.perform(post("/api/v1/bar/bills/" + bill + "/settle").with(cashierA)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private JsonNode data(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).at("/data");
    }

    private static Map<String, JsonNode> rows(JsonNode board) {
        Map<String, JsonNode> rows = new LinkedHashMap<>();
        List<JsonNode> list = new ArrayList<>();
        board.at("/orders").forEach(list::add);
        list.forEach(row -> rows.put(row.at("/order_id").asText(), row));
        return rows;
    }

    private static String read(ResultActions result, String path) throws Exception {
        return CustomerTicketPaymentFixture.read(result, path);
    }
}
