package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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

/**
 * 第十八批 AU 段第 283 条 283-R1（真库）：BSM:1064「结账时累计消费多出的不退、不足最低消费的要补足……结账补足的差额按普通消费计入」；BSM:1079 转台
 * 「按目标台最低消费额重新计算完成进度，不足部分继续消费」、拆台后「各结果台显示差额并按既有规则继续消费或结账补足」。包厢开台下一单达本台低消、生成未付账单后
 * 转到低消更高的空包厢：订单列表账单摘要带出服务端算的低消差额（与结账闸同一算法），结账 409；按差额补足后差额为 0、应付增加，结账成功；已结账单不带差额。
 * 不开测试事务（数据带纯数字后缀留在一次性测试库）；请求按后台卡座页、订单页真实形状（蛇形键、X-Terminal-Type: web）经控制器与真实授权链提交。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class BarMinSpendTopUpJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BarRepository bars;

    private final String s = Long.toString(System.nanoTime());
    private final String project = "prj_mst_" + s;
    private final String company = "ent_mst_" + s;
    private final String bar = "bar_mst_" + s;
    private final String wine = "product_mst_" + s;
    private final String waiter = "usr_mst_waiter_" + s;
    private final AtomicInteger seq = new AtomicInteger();
    private RequestPostProcessor clerk;
    private RequestPostProcessor supervisor;

    @BeforeEach
    void seed() throws Exception {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','低消补足综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'低消补足酒吧','bar',?,?,?,?,'active',now(),now())", bar, company, company, company, project);
        // 包厢开账单要有该营业日已批准的收费规则（服务费率取 0，账单金额即消费额）。
        jdbc.update("""
                INSERT INTO bar_payroll_rules(id, rule_kind, project_id, merchant_id, effective_from, version, rule_json, reason, status,
                    platform_company_id, created_by)
                VALUES (?, 'charge', ?, NULL, ?, 'V0.01', ?::jsonb, '服务费 0', 'approved', ?, 'usr_mst')
                """, "bpr_mst_" + s, project, java.sql.Date.valueOf(LocalDate.now().minusDays(1)),
                json.writeValueAsString(new BarPayrollRequests.RuleValues(0, 0, 2000, null, null, null, List.of())), company);
        Instant now = Instant.now();
        bars.saveProduct(new BarProductRecord(wine, bar, bar, "干红", "wine", 10000L, "CNY", null, null, false, "active",
                now, now, null, null, null, false, null, null, null));
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        clerk = staffs.staff("usr_mst_clerk_" + s, "staff", project, bar, "bar.table_session.create", "bar.table_session.view",
                "bar.table_session.transfer", "bar.order.create", "bar.order.view", "finance.payment.register", "bar.bill.adjust",
                "bar.table_commission.calculate");
        // 账单调整（含补足低消）走既有准入闸 bar.bill_waiver（V068 恒阻断、上一级审批）：本酒吧主管为收银的直接上级。
        String supervisorId = "usr_mst_sup_" + s;
        supervisor = staffs.staff(supervisorId, "staff", project, bar,
                "governance.supervisor_review.view", "governance.supervisor_review.approve", "governance.supervisor_review.manage");
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id) VALUES (?, ?)", "usr_mst_clerk_" + s, supervisorId);
        // 目标包厢接手服务员：在岗员工档案、本商户点单工作权限，转台前排目标包厢点位此刻的班。
        staffs.staff(waiter, "staff", project, bar, "bar.order.create");
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,company_id,registration_status,status,employment_status,"
                + "is_sales_position,created_at,updated_at) VALUES(?,?,?,?,?,?,'approved','active','employed',false,now(),now())",
                "pro_mst_" + s, project, bar, waiter, "接手服务员" + s, company);
    }

    /**
     * 包厢 A（低消 100 元）开台下单 100 元并生成未付账单，转到低消 400 元的空包厢 B：摘要差额 300 元、结账 409「请先加购消费或补足低消差额再结账」；
     * 补足 300 元（首次提交照既有准入闸 409 转主管复核，直接上级批准后原样再提交）后差额 0、应付 400 元，全额结账成功；结清后摘要不带差额。
     */
    @Test
    void transferToHigherMinimumShowsGapBlocksSettleAndTopUpClearsIt() throws Exception {
        String roomA = room(10000L);
        String roomB = room(40000L);
        String session = open(roomA);
        String order = order(roomA);
        String bill = bill(order);
        transfer(session, roomB).andExpect(status().isOk());

        JsonNode summary = summary(order);
        assertThat(summary.path("id").asText()).isEqualTo(bill);
        assertThat(summary.path("min_spend_gap_cent").asLong(-1)).isEqualTo(30000L);
        assertThat(summary.path("payable_amount_cent").asLong()).isEqualTo(10000L);
        settle(bill, 10000L).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_MIN_CONSUMPTION_NOT_MET"));

        Map<String, Object> topUp = new LinkedHashMap<>();
        topUp.put("idempotency_key", "mst-topup-" + s);
        topUp.put("adjustment_type", "min_spend_top_up");
        topUp.put("amount_cent", 30000);
        topUp.put("reason", "结账补足低消差额");
        post(clerk, "/api/v1/bar/bills/" + bill + "/adjustments", topUp).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED"));
        assertThat(summary(order).path("min_spend_gap_cent").asLong(-1)).isEqualTo(30000L);
        String review = jdbc.queryForObject("SELECT review_id FROM supervisor_review_decisions WHERE action_type = 'bar.bill_waiver' AND object_id = ?"
                + " AND review_status = 'pending_review'", String.class, bill);
        post(supervisor, "/api/v1/platform/supervisor-review/" + review + "/chain/regenerate", Map.of()).andExpect(status().isOk());
        post(supervisor, "/api/v1/platform/supervisor-review/" + review + "/approve", Map.of(
                "idempotency_key", "mst-approve-" + s, "decision", "approved", "comment", "核实转大包厢补足低消")).andExpect(status().isOk());
        post(clerk, "/api/v1/bar/bills/" + bill + "/adjustments", topUp).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adjustment_type").value("min_spend_top_up"));
        summary = summary(order);
        assertThat(summary.path("min_spend_gap_cent").asLong(-1)).isEqualTo(0L);
        assertThat(summary.path("payable_amount_cent").asLong()).isEqualTo(40000L);

        settle(bill, 40000L).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT settlement_status FROM bar_bills WHERE id = ?", String.class, bill)).isEqualTo("settled");
        assertThat(summary(order).hasNonNull("min_spend_gap_cent")).isFalse();
    }

    /**
     * 第十八批 AU 段收尾复核（BSM:1064「结账补足的差额按普通消费计入该商户营业额、营业额抽成基数与销售提成基数」；ENUM:117 同义；决策台账 B-08 操作员答复
     * 「补足差额按普通消费计入营业额、抽成和提成基数」）：转到低消 400 元的包厢后补足 300 元、结清 400 元，账单挂上销售归属后按后台提成页计算提成，
     * 提成基数＝小计 100 元＋补足 300 元＝400 元。修复前红：补足额不进提成基数，基数只有 100 元。
     */
    @Test
    void toppedUpGapCountsTowardSalesCommissionBase() throws Exception {
        String roomA = room(10000L);
        String roomB = room(40000L);
        String session = open(roomA);
        String order = order(roomA);
        String bill = bill(order);
        transfer(session, roomB).andExpect(status().isOk());
        approvedTopUp(bill, 30000L);
        settle(bill, 40000L).andExpect(status().isOk());
        jdbc.update("UPDATE bar_bills SET sales_staff_id = ?, sales_role_type = 'non_sales' WHERE id = ?", waiter, bill);

        post(clerk, "/api/v1/bar/bills/" + bill + "/commissions/calculate", Map.of("idempotency_key", "mst-commission-" + s))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commission_base_cent").value(40000));
        assertThat(jdbc.queryForObject("SELECT commission_base_cent FROM bar_commission_calculations WHERE bill_id = ?", Long.class, bill))
                .isEqualTo(40000L);
    }

    // ---------- helpers ----------

    /** 后台订单页「补足低消差额」：首次提交照既有准入闸 409 转主管复核，直接上级批准后原样再提交生效。 */
    private void approvedTopUp(String bill, long amountCent) throws Exception {
        Map<String, Object> topUp = new LinkedHashMap<>();
        topUp.put("idempotency_key", "mst-topup-" + s);
        topUp.put("adjustment_type", "min_spend_top_up");
        topUp.put("amount_cent", amountCent);
        topUp.put("reason", "结账补足低消差额");
        post(clerk, "/api/v1/bar/bills/" + bill + "/adjustments", topUp).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED"));
        String review = jdbc.queryForObject("SELECT review_id FROM supervisor_review_decisions WHERE action_type = 'bar.bill_waiver' AND object_id = ?"
                + " AND review_status = 'pending_review'", String.class, bill);
        post(supervisor, "/api/v1/platform/supervisor-review/" + review + "/chain/regenerate", Map.of()).andExpect(status().isOk());
        post(supervisor, "/api/v1/platform/supervisor-review/" + review + "/approve", Map.of(
                "idempotency_key", "mst-approve-" + s, "decision", "approved", "comment", "核实转大包厢补足低消")).andExpect(status().isOk());
        post(clerk, "/api/v1/bar/bills/" + bill + "/adjustments", topUp).andExpect(status().isOk());
    }

    /** 本项目本商户铺位上的 4 人包厢（空闲），低消按参数。 */
    private String room(long minConsumptionCent) {
        String space = "spc_mst_" + s + "_" + seq.incrementAndGet();
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,area_square_meter,status,created_at,updated_at) "
                + "VALUES(?,?,?,'包厢区','unit',20,'active',now(),now())", space, project, bar);
        String id = "tbl_mst_" + s + "_" + seq.incrementAndGet();
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,capacity,allowed_pass_free_guest_count,"
                + "min_consumption_cent,currency,status,created_at,updated_at,space_id) VALUES(?,?,?,?,'4人包厢','vip',4,4,?,'CNY','active',now(),now(),?)",
                id, bar, bar, "V" + id, minConsumptionCent, space);
        return id;
    }

    /** 后台卡座页「开台」：体 idempotency_key、bar_id、table_id、guest_count。 */
    private String open(String table) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "mst-open-" + seq.incrementAndGet() + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", table);
        body.put("guest_count", 2);
        return CustomerTicketPaymentFixture.read(post(clerk, "/api/v1/bar/table-sessions", body).andExpect(status().isOk()), "$.data.id");
    }

    private String order(String table) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "mst-order-" + seq.incrementAndGet() + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", table);
        body.put("items", List.of(Map.of("product_id", wine, "quantity", 1)));
        return CustomerTicketPaymentFixture.read(post(clerk, "/api/v1/bar/orders", body).andExpect(status().isOk()), "$.data.id");
    }

    private String bill(String order) throws Exception {
        return CustomerTicketPaymentFixture.read(post(clerk, "/api/v1/bar/orders/" + order + "/bill",
                Map.of("idempotency_key", "mst-bill-" + s)).andExpect(status().isOk()), "$.data.id");
    }

    /** 后台卡座页「转台」：目标包厢接手人有目标包厢点位此刻的已发布班次。 */
    private ResultActions transfer(String session, String target) throws Exception {
        Instant now = Instant.now();
        jdbc.update("INSERT INTO staff_shift_schedules(id,user_id,work_date,start_at,end_at,point_space_ids,created_by,project_id) "
                + "VALUES(?,?,?,?,?,jsonb_build_array(?::text),?,?)", "sft_mst_" + s + "_" + seq.incrementAndGet(), waiter, LocalDate.now(),
                Timestamp.from(now.minus(Duration.ofHours(2))), Timestamp.from(now.plus(Duration.ofHours(4))),
                jdbc.queryForObject("SELECT space_id FROM bar_tables WHERE id = ?", String.class, target), "usr_mst_clerk_" + s, project);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "mst-transfer-" + seq.incrementAndGet() + "-" + s);
        body.put("target_table_id", target);
        body.put("target_service_staff_id", waiter);
        body.put("reason", "客人换大包厢");
        body.put("session_version", jdbc.queryForObject("SELECT session_version FROM bar_table_sessions WHERE id = ?", Long.class, session));
        return post(clerk, "/api/v1/bar/table-sessions/" + session + "/transfer", body);
    }

    /** 后台订单页结账：现金登记，payment_id 取本次登记的幂等键。 */
    private ResultActions settle(String bill, long amountCent) throws Exception {
        String key = "mst-settle-" + seq.incrementAndGet() + "-" + s;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("payment_id", key);
        body.put("settlement_amount_cent", amountCent);
        body.put("payment_channel", "cash");
        return post(clerk, "/api/v1/bar/bills/" + bill + "/settle", body);
    }

    /** 后台订单页读订单列表（GET /api/v1/bar/orders）：该单第一张账单摘要。 */
    private JsonNode summary(String order) throws Exception {
        String body = mockMvc.perform(get("/api/v1/bar/orders").with(clerk)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        for (JsonNode item : json.readTree(body).path("data").path("items")) {
            if (order.equals(item.path("id").asText())) {
                return item.path("bills").path(0);
            }
        }
        throw new AssertionError("订单列表里没有 " + order);
    }

    private ResultActions post(RequestPostProcessor who, String path, Map<String, Object> body) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
}
