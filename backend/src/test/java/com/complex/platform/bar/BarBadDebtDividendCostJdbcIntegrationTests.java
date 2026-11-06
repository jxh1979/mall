package com.complex.platform.bar;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.DailyCloseFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
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
 * 第十八批 T 段第 157 条 157-R8（真库）：第十八批裁决 38 选项原文「收入与出库成本都不进分红净利润」；
 * BSM:1111「转坏账核销的订单不产生销售提成，也不计入分红净利润基数」。
 * 包厢挂起单出品后经理确认坏账：分红计算单的销货成本另列一行冲回该单出库成本，未转坏账的挂起单出库成本照常计入；
 * 坏账单已收的部分也不进酒吧收入（157-R3）。请求按后台真实形状（蛇形键、X-Terminal-Type: web）经控制器与真实授权链提交。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarBadDebtDividendCostJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BarRepository bars;
    @Autowired private BusinessDayResolver days;

    private String s;
    private String bar;
    private String wine;
    private RequestPostProcessor cashier;
    private RequestPostProcessor manager;
    private RequestPostProcessor admin;

    @BeforeEach
    void seed() throws Exception {
        s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String project = "prj_bdd_" + s;
        String company = "ent_bdd_" + s;
        bar = "bar_bdd_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','坏账分红综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'坏账分红酒吧','bar',?,?,?,?,'active',now(),now())", bar, company, company, company, project);
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) VALUES(?,?,?,'包厢区','unit','active',now(),now())",
                "space_bdd_" + s, project, bar);
        // 包厢开账单要有该营业日已批准的收费规则（服务费率取 0，账单金额即消费额）。
        jdbc.update("""
                INSERT INTO bar_payroll_rules(id, rule_kind, project_id, merchant_id, effective_from, version, rule_json, reason, status,
                    platform_company_id, created_by)
                VALUES (?, 'charge', ?, NULL, ?, 'V0.01', ?::jsonb, '服务费 0', 'approved', ?, 'usr_bdd')
                """, "bpr_bdd_" + s, project, java.sql.Date.valueOf(LocalDate.now().minusDays(1)),
                json.writeValueAsString(new BarPayrollRequests.RuleValues(0, 0, 2000, null, null, null, List.of())), company);
        // 整瓶出品扣库存：吧台 10 瓶，移动加权成本每瓶 8000；最近到货价 80 元为低价值（不走主管复核）。
        String item = "item_bdd_" + s;
        String location = "loc_bdd_" + s;
        String counter = "wh_bdd_" + s;
        jdbc.update("INSERT INTO warehouses (id, project_id, merchant_id, warehouse_type, name) VALUES (?, ?, ?, 'bar_counter', '包厢吧台')", counter, project, bar);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, '干红 750 毫升', '酒类', '瓶', 'active', ?, now(), now())", item, bar);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id, project_id)"
                + " VALUES (?, ?, ?, 10, 0, 10, now(), ?, ?, ?)", "bal_bdd_" + s, item, location, bar, counter, project);
        jdbc.update("INSERT INTO inventory_item_cost_state(item_id,merchant_id,on_hand_quantity,total_cost_cent,avg_unit_cost_cent,has_cost_basis)"
                + " VALUES(?,?,10,80000,8000,TRUE)", item, bar);
        jdbc.update("INSERT INTO inventory_stock_movements (id, movement_type, item_id, target_location_id, quantity, business_type, status,"
                + " created_at, merchant_id, project_id, unit_cost_cent, total_cost_cent) VALUES (?, 'inbound', ?, ?, 1, 'purchase_request', 'completed',"
                + " now() - interval '1 day', ?, ?, 8000, 8000)", "arr_bdd_" + s, item, location, bar, project);
        Instant now = Instant.now();
        wine = "product_bdd_wine_" + s;
        bars.saveProduct(new BarProductRecord(wine, bar, bar, "干红", "wine", 30000L, "CNY", item, location, true, "active",
                now, now, null, null, null, false, null, null, null));
        // 分红方案：月度、只计酒吧收入、扣销货成本。
        jdbc.update("INSERT INTO dividend_plans (id, merchant_id, principal_user_id, period_type, revenue_domains, include_inventory_cost, status)"
                + " VALUES (?, ?, 'usr_bdd_principal', 'monthly', 'bar', TRUE, 'active')", "divp_bdd_" + s, bar);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        cashier = staffs.staff("usr_bdd_cashier_" + s, "staff", project, bar, "bar.order.create", "finance.payment.register", "bar.bill.hang",
                "bar.table.open_close");
        manager = staffs.staff("usr_bdd_manager_" + s, "staff", project, bar, "bar.bill.hang_cancel");
        admin = staffs.staff("usr_bdd_admin_" + s, "platform_admin", project, null, "finance.dividend.manage");
    }

    /**
     * 两张包厢挂起单：甲出品 2 瓶、收了 200 元后转坏账，乙出品 1 瓶仍挂起未付。分红计算单销货成本先按全部出库 24000 列，
     * 再另列一行冲回甲的出库成本 16000（说明引裁决 38），计算单销货成本 8000；甲已收部分不进酒吧收入。
     */
    @Test
    void badDebtOrderOutboundCostIsExcludedFromDividendCost() throws Exception {
        String badOrder = orderId(order(table("vipa"), 2));
        String badBill = bill(badOrder, "a");
        deliver(badOrder).andExpect(status().isOk());
        settle(badBill, 20000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlement_status").value("partial_settled"));
        suspend(badOrder).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/bar/orders/" + badOrder + "/suspension/bad-debt").with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bdd-bad-" + s, "reason", "客人失联，确认无法收回"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.inventory_cost_cent").value(16000));
        String keptOrder = orderId(order(table("vipb"), 1));
        bill(keptOrder, "b");
        deliver(keptOrder).andExpect(status().isOk());
        suspend(keptOrder).andExpect(status().isOk());

        // 第十八批 V 段 159-R5：正式分红单要求期间内每个营业日五类已标最终；计算前把当月逐日标齐（直插待确认日结，不锁账）。
        YearMonth month = YearMonth.from(days.businessDate(null, bar, Instant.now()));
        DailyCloseFixture.markRangeFinal(jdbc, "bar", bar, month.atDay(1), month.atEndOfMonth());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bdd-div-" + s);
        body.put("period_label", month.toString());
        body.put("kind", "official");
        body.put("merchant_id", bar);
        String statement = data(mockMvc.perform(post("/api/v1/finance/dividends/statements/compute").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed.length()").value(1))
                .andExpect(jsonPath("$.data.computed[0].cost_cent").value(8000))
                .andExpect(jsonPath("$.data.computed[0].revenue_cent").value(0)), "$.data.computed[0].id");
        List<Map<String, Object>> costLines = jdbc.queryForList("SELECT amount_cent, detail FROM dividend_statement_lines"
                + " WHERE statement_id = ? AND line_type = 'cost_inventory' ORDER BY seq", statement);
        assertThat(costLines).extracting(row -> ((Number) row.get("amount_cent")).longValue()).containsExactly(-24000L, 16000L);
        assertThat((String) costLines.get(1).get("detail")).contains("坏账").contains("裁决 38");
    }

    // ---------- helpers ----------

    private String table(String tag) {
        String id = "table_bdd_" + tag + "_" + s;
        Instant now = Instant.now();
        bars.saveTable(new BarTableRecord(id, bar, bar, "T-" + tag + "-" + s, tag, "vip", 4, 0, 0L, "CNY", "opened", now, now));
        bars.assignTableSpace(id, "space_bdd_" + s);
        bars.saveTableSession(new BarTableSessionRecord("session_bdd_" + tag + "_" + s, bar, bar, LocalDate.now(), id, null, null, 2,
                0L, 0, null, null, "walk_in", "open", now, null, null, 1L, now, now));
        return id;
    }

    private ResultActions order(String table, int quantity) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bdd-order-" + UUID.randomUUID());
        body.put("bar_id", bar);
        body.put("table_id", table);
        body.put("items", List.of(Map.of("product_id", wine, "quantity", quantity)));
        return mockMvc.perform(post("/api/v1/bar/orders").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private String bill(String order, String tag) throws Exception {
        return data(mockMvc.perform(post("/api/v1/bar/orders/" + order + "/bill").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "bdd-bill-" + tag + "-" + s)))).andExpect(status().isOk()),
                "$.data.id");
    }

    private ResultActions deliver(String order) throws Exception {
        List<Map<String, Object>> lines = jdbc.queryForList("SELECT id, quantity FROM bar_order_items WHERE order_id = ?", order).stream()
                .map(row -> Map.<String, Object>of("order_item_id", row.get("id"), "quantity", row.get("quantity"))).toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bdd-deliver-" + order);
        body.put("operator_note", "已送达");
        body.put("delivery_photo_attachment_ids", List.of());
        body.put("delivered_items", lines);
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/deliver").with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions suspend(String order) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/suspend").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "bdd-hang-" + order, "reason", "客人稍后付款"))));
    }

    private ResultActions settle(String bill, long amountCent) throws Exception {
        String key = "bdd-settle-" + UUID.randomUUID();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("payment_id", key);
        body.put("settlement_amount_cent", amountCent);
        body.put("payment_channel", "cash");
        return mockMvc.perform(post("/api/v1/bar/bills/" + bill + "/settle").with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private String orderId(ResultActions result) throws Exception {
        return data(result.andExpect(status().isOk()), "$.data.id");
    }

    private static String data(ResultActions result, String path) throws Exception {
        return CustomerTicketPaymentFixture.read(result, path);
    }
}
