package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * 第十八批 AF 段第 198 条 198-R5（酒吧）：契约删去 BarBillSettleRequest.operator_note（结账从不读；后台订单页 BarOrdersPage 与安卓员工端
 * 停发写死的备注）后，收银员按订单页现金结账的新形状——payment_id、payment_channel、idempotency_key——走真实 HTTP、真实授权链结清账单。
 * 入口未改，本类只锁现行为，不先红。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarBillSettleWebShape198R5Tests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BarRepository bars;

    private final String s = String.valueOf(System.nanoTime());
    private final String bar = "bar_b52ws_" + s;
    private final String table = "table_b52ws_" + s;
    private final String snack = "product_b52ws_" + s;
    private final String cashierId = "usr_b52ws_cash_" + s;
    private RequestPostProcessor cashier;

    @BeforeEach
    void seed() throws Exception {
        String project = "prj_b52ws_" + s;
        String company = "ent_b52ws_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','结账形状综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'结账形状酒吧','bar',?,?,?,?,'active',now(),now())", bar, company, company, company, project);
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) VALUES(?,?,?,'散台区','unit','active',now(),now())",
                "space_b52ws_" + s, project, bar);
        Instant now = Instant.now();
        bars.saveProduct(new BarProductRecord(snack, bar, bar, "果盘", "snack", 300L, "CNY", null, null, false, "active",
                now, now, null, null, null, false, null, null, null));
        bars.saveTable(new BarTableRecord(table, bar, bar, "T-b52ws-" + s, "散台", "loose_table", 4, 0, 0L, "CNY", "opened", now, now));
        bars.assignTableSpace(table, "space_b52ws_" + s);
        bars.saveTableSession(new BarTableSessionRecord("session_b52ws_" + s, bar, bar, LocalDate.now(), table, null, null, 2,
                0L, 0, null, null, "walk_in", "open", now, null, null, 1L, now, now));
        jdbc.update("""
                INSERT INTO bar_payroll_rules(id, rule_kind, project_id, merchant_id, effective_from, version, rule_json, reason, status,
                    platform_company_id, created_by)
                VALUES (?, 'charge', ?, NULL, ?, 'V0.01', ?::jsonb, '服务费 0', 'approved', ?, 'usr_b52ws')
                """, "bpr_b52ws_" + s, project, java.sql.Date.valueOf(LocalDate.now().minusDays(1)),
                json.writeValueAsString(new BarPayrollRequests.RuleValues(0, 0, 2000, null, null, null, List.of())), company);
        cashier = new StaffRequestFixture(jdbc).staff(cashierId, "staff", project, bar,
                "bar.order.create", "bar.shift.manage", "finance.payment.register");
    }

    // 订单页现金结账新形状（不带 operator_note）：200，账单结清、实收等于应付。
    @Test
    void settleBill_webShapeCashWithoutOperatorNote_ok() throws Exception {
        call("/api/v1/bar/shifts/open", Map.of("idempotency_key", "b52ws-shift-" + s, "bar_id", bar,
                "cashier_user_id", cashierId, "opening_cash_cent", 0)).andExpect(status().isOk());
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("idempotency_key", "b52ws-order-" + s);
        order.put("bar_id", bar);
        order.put("table_id", table);
        order.put("items", List.of(Map.of("product_id", snack, "quantity", 2)));
        String orderId = JsonPath.read(call("/api/v1/bar/orders", order).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        String billId = JsonPath.read(call("/api/v1/bar/orders/" + orderId + "/bill",
                        Map.of("idempotency_key", "b52ws-bill-" + s, "operator_note", "完整后台验收账单"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");

        call("/api/v1/bar/bills/" + billId + "/settle", Map.of("payment_id", "b52ws-cash-" + s, "payment_channel", "cash",
                "idempotency_key", "b52ws-settle-" + s))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlement_status").value("settled"));
        assertThat(jdbc.queryForMap("SELECT settlement_status, paid_amount_cent FROM bar_bills WHERE id = ?", billId))
                .containsEntry("settlement_status", "settled")
                .containsEntry("paid_amount_cent", 600L);
    }

    private ResultActions call(String path, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post(path).with(cashier).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
}
