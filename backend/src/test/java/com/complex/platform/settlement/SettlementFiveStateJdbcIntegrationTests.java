package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AL 段第 240 条 240-R1、240-R3 真库＋真实接口回归。依据：ENUM:192「结算状态 草稿、待复核、已确认、已调整、已归档」；
 * 第十八批裁决 54 所选选项原文「通票月结单、通票流水（随所属月结单）、演出结算、积分月结账单按五态显示：未出结果或未出账→草稿；
 * 演出挂起、积分账单有未处理异议→待复核；已出结果或已入账→已确认；被财务调整取代的演出行→已调整；所属综合体该月已封账（裁决 41）→已归档」；
 * 第十八批裁决 64「只有本身已确认或已调整、且该月已封账的才显示已归档；还没出账的照常显示草稿、还有异议或挂起的照常显示待复核」。
 *
 * <p>四类对象各多返回派生字段 settlement_state（只是显示口径，不改原状态与流转）。夹具直接写各对象的现有状态行（月结单、核销结算流水、
 * 演出结算行、积分月账单与异议）与封账行，随测试事务回滚；年份取 2091，远离他测留库的平台单。请求按前端真实地址、真实授权链、蛇形键。
 * 修复前接口没有 settlement_state，第一处断言即红。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SettlementFiveStateJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = MediaPerformanceTestFixtures.suffix();
    private final String project = "p240_" + s;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // ① 通票：M 月中午一笔、M+1 月 1 日北京 05:00 一笔（出厂营业日 10:00 起算，属 M 月最后一个营业日）。未出单→草稿；
    //    出 M 月单（算出即已确认）→单与两笔流水都已确认；该综合体 M 月封账→都已归档。
    @Test
    void passBillAndTransactionsFollowBillThenPeriodClose() throws Exception {
        MediaPerformanceTestFixtures.seedProject(jdbc, project);
        String merchant = "m240_" + s;
        MediaPerformanceTestFixtures.seedMerchant(jdbc, merchant, "catering", project);
        String noon = transaction("noon", merchant, Instant.parse("2091-03-15T04:00:00Z"));
        String dawn = transaction("dawn", merchant, Instant.parse("2091-03-31T21:00:00Z"));
        RequestPostProcessor viewer = MediaPerformanceTestFixtures.staff(jdbc, "u240t_" + s, project, null, "finance.settlement.view");
        String transactions = "/api/v1/settlement/transactions?projectId=" + project;

        transactionStates(viewer, transactions, noon, dawn, "draft");

        String bill = "sb240_" + s;
        jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status, created_at, updated_at)"
                + " VALUES (?, ?, DATE '2091-03-01', DATE '2091-03-31', 'confirmed', now(), now())", bill, project);
        mockMvc.perform(get("/api/v1/settlement/bills?projectId=" + project + "&month=2091-03").with(viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(bill))
                .andExpect(jsonPath("$.data[0].status").value("confirmed"))
                .andExpect(jsonPath("$.data[0].settlement_state").value("confirmed"));
        transactionStates(viewer, transactions, noon, dawn, "confirmed");

        closePeriod("2091-03-01");
        mockMvc.perform(get("/api/v1/settlement/bills?projectId=" + project + "&month=2091-03").with(viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].settlement_state").value("archived"));
        transactionStates(viewer, transactions, noon, dawn, "archived");
    }

    // ② 演出：现行已锁定版本上挂起→待复核；被新版本取代的版本上残留的挂起→草稿；已出账→已确认；被财务调整取代的原行→已调整、调整后新行→已确认。
    //    该场项目、演出月封账后，已出账与已调整的行→已归档，挂起的照常待复核、被取代版本的照常草稿（裁决 64）。
    @Test
    void mediaSettlementRowsCarryFiveStates() throws Exception {
        MediaPerformanceTestFixtures.seedProject(jdbc, project);
        String media = "mm240_" + s;
        MediaPerformanceTestFixtures.seedMerchant(jdbc, media, "media", project);
        String group = "ms240_" + s;
        for (Object[] v : new Object[][] {{1, "superseded"}, {2, "locked"}}) {
            jdbc.update("INSERT INTO media_performance_shows(id,show_group_id,contract_version,project_id,media_merchant_id,show_kind,show_date,status,locked_at,locked_by,created_by)"
                    + " VALUES(?,?,?,?,?,'self_hosted',DATE '2091-05-20',?,now(),'seed','seed')", group + "_v" + v[0], group, v[0], project, media, v[1]);
        }
        String v1 = group + "_v1";
        String v2 = group + "_v2";
        settlement(v1 + "_old", v1, "fixed_fee", "pending", "contract_silent", null);
        settlement(v2 + "_pending", v2, "ticket_share", "pending", "unconfirmed", null);
        settlement(v2 + "_posted", v2, "fixed_fee", "posted", null, null);
        settlement(v2 + "_adjusted", v2, "drink_commission", "adjusted", null, null);
        settlement(v2 + "_new", v2, "drink_commission", "posted", null, v2 + "_adjusted");
        RequestPostProcessor viewer = MediaPerformanceTestFixtures.staff(jdbc, "u240m_" + s, project, null, "media.performance.view");

        mediaState(viewer, v1, v1 + "_old", "draft");
        mediaState(viewer, v2, v2 + "_pending", "pending_review");
        mediaState(viewer, v2, v2 + "_posted", "confirmed");
        mediaState(viewer, v2, v2 + "_adjusted", "adjusted");
        mediaState(viewer, v2, v2 + "_new", "confirmed");

        closePeriod("2091-05-01");
        mediaState(viewer, v1, v1 + "_old", "draft");
        mediaState(viewer, v2, v2 + "_pending", "pending_review");
        mediaState(viewer, v2, v2 + "_posted", "archived");
        mediaState(viewer, v2, v2 + "_adjusted", "archived");
        mediaState(viewer, v2, v2 + "_new", "archived");
    }

    // ③ 积分月结账单：未公布→草稿；已公布、有未处理异议→待复核；已公布、异议已处理→已确认。该账单项目、计费月封账后，
    //    已确认的→已归档，未公布的照常草稿、有未处理异议的照常待复核（裁决 64）。
    @Test
    void ticketPointsBillsCarryFiveStates() throws Exception {
        MediaPerformanceTestFixtures.seedProject(jdbc, project);
        String merchant = "mp240_" + s;
        MediaPerformanceTestFixtures.seedMerchant(jdbc, merchant, "bar", project);
        jdbc.update("""
                INSERT INTO ticket_points_billing_rules (id, version, effective_month, statement_day, objection_working_days, minimum_days,
                    maximum_days, pay_days, status, created_by, created_at, approved_at)
                VALUES (?, ?, DATE '2091-07-01', 1, 10, 1, 30, '{5,15,25}', 'approved', 'seed', now(), now()) ON CONFLICT DO NOTHING
                """, "tpbr240_" + s, "V240" + s);
        String rule = jdbc.queryForObject("SELECT id FROM ticket_points_billing_rules WHERE effective_month = DATE '2091-07-01'", String.class);
        String receiver = entity("receiver");
        String unpublished = pointsBill("draft", rule, receiver, merchant, false);
        String objected = pointsBill("open", rule, receiver, merchant, true);
        String settled = pointsBill("done", rule, receiver, merchant, true);
        objection(objected, "open");
        objection(settled, "resolved");
        RequestPostProcessor viewer = MediaPerformanceTestFixtures.staff(jdbc, "u240p_" + s, project, merchant, "ticket.points.billing.view");

        pointsState(viewer, unpublished, "draft");
        pointsState(viewer, objected, "pending_review");
        pointsState(viewer, settled, "confirmed");

        closePeriod("2091-07-01");
        pointsState(viewer, unpublished, "draft");
        pointsState(viewer, objected, "pending_review");
        pointsState(viewer, settled, "archived");
    }

    // ④ 240-R3：指标明细页结算三指标的「结算状态」列同结算审阅页——随所属月结单派生、行里不再带流水原值 settlementStatus。
    @Test
    void metricDetailSettlementRowsCarryFiveStates() throws Exception {
        MediaPerformanceTestFixtures.seedProject(jdbc, project);
        String merchant = "ma240_" + s;
        MediaPerformanceTestFixtures.seedMerchant(jdbc, merchant, "catering", project);
        String row = transaction("metric", merchant, Instant.parse("2091-09-10T04:00:00Z"));
        jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status, created_at, updated_at)"
                + " VALUES (?, ?, DATE '2091-09-01', DATE '2091-09-30', 'settled', now(), now())", "sb240a_" + s, project);
        RequestPostProcessor viewer = MediaPerformanceTestFixtures.staff(jdbc, "u240a_" + s, project, null,
                "analytics.dashboard.view", "finance.settlement.view");
        String url = "/api/v1/analytics/metric-detail?metric=settlement.transaction_count&page=1&size=100";

        mockMvc.perform(get(url).with(viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[?(@.id == 'st_vs_" + row + "')].settlementState").value("confirmed"))
                .andExpect(jsonPath("$.data.rows[?(@.id == 'st_vs_" + row + "')].settlementStatus").isEmpty())
                .andExpect(jsonPath("$.data.columns[?(@.key == 'settlementState')].label").value("结算状态"));

        closePeriod("2091-09-01");
        mockMvc.perform(get(url).with(viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[?(@.id == 'st_vs_" + row + "')].settlementState").value("archived"));
    }

    // ── 夹具 ───────────────────────────────────────────────────────────────────────────────

    /** 本餐厅一笔核销结算流水（写法同 TicketSettlementBusinessMonthJdbcTests.seedExclusiveFact）。 */
    private String transaction(String tag, String merchant, Instant businessAt) {
        String ver = "v240_" + tag + "_" + s;
        Timestamp ts = Timestamp.from(businessAt);
        jdbc.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    verification_id, entitlement_type, verified_count, total_times, num_people,
                    allocated_revenue_cent, verified_amount_cent, unverified_amount_cent, beneficiary_type,
                    merchant_id, project_id, exclusive_merchant_id, settlement_status, entitlement_valid_to,
                    created_at, updated_at, business_at)
                VALUES (?, ?, 'verification_settled', ?, ?, 'dining', 1, 1, 1, 3000, 3000, 0, 'merchant',
                    ?, ?, ?, 'draft', ?, ?, ?, ?)
                """, "st_vs_" + ver, "ST-VS-" + ver, "ent_" + ver, ver, merchant, project, merchant, ts, ts, ts, ts);
        return ver;
    }

    private void transactionStates(RequestPostProcessor viewer, String url, String a, String b, String expected) throws Exception {
        ResultActions result = mockMvc.perform(get(url).with(viewer)).andExpect(status().isOk());
        for (String ver : new String[] {a, b}) {
            result.andExpect(jsonPath("$.data.items[?(@.id == 'st_vs_" + ver + "')].settlement_state").value(expected));
        }
    }

    private void closePeriod(String month) {
        jdbc.update("INSERT INTO finance_period_closes (id, project_id, period_month, closed_by, closed_at) VALUES (?, ?, CAST(? AS date), 'seed', now())",
                "fpc240_" + month + "_" + s, project, month);
    }

    private void settlement(String id, String show, String feeKind, String status, String pendingReason, String adjusts) {
        jdbc.update("INSERT INTO media_performance_settlements(id,show_id,fee_kind,status,pending_reason,adjusts_settlement_id,adjust_reason,created_by)"
                + " VALUES(?,?,?,?,?,?,?,'seed')", id, show, feeKind, status, pendingReason, adjusts, adjusts == null ? null : "财务调整");
    }

    private void mediaState(RequestPostProcessor viewer, String show, String settlementId, String expected) throws Exception {
        mockMvc.perform(get(MediaPerformanceTestFixtures.BASE + "/" + show).with(viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlements[?(@.id == '" + settlementId + "')].settlement_state").value(expected));
    }

    private String entity(String tag) {
        String id = "be240_" + tag + "_" + s;
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) "
                + "VALUES (?, ?, 'platform', 'active', now(), now())", id, id);
        return id;
    }

    /** 2091-07 计费月的一张积分月账单；published 为真时已公布（异议期与付款日已定）。卖方主体逐张不同，避开账单范围唯一索引。 */
    private String pointsBill(String tag, String rule, String receiver, String merchant, boolean published) {
        String id = "tpmb240_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO ticket_points_monthly_bills (id, billing_month, billing_rule_id, seller_entity_id, seller_merchant_id, receiver_entity_id,
                    project_id, amount_cent, created_at, visible_at, objection_deadline, due_date)
                VALUES (?, DATE '2091-07-01', ?, ?, ?, ?, ?, 100, now(), ?, ?, ?)
                """, id, rule, entity("seller_" + tag), merchant, receiver, project,
                published ? Timestamp.from(Instant.parse("2091-08-01T02:00:00Z")) : null,
                published ? java.sql.Date.valueOf("2091-08-15") : null, published ? java.sql.Date.valueOf("2091-08-25") : null);
        return id;
    }

    private void objection(String bill, String status) {
        jdbc.update("INSERT INTO ticket_points_bill_objections (id, bill_id, reason, submitted_by, submitted_at, status) VALUES (?, ?, '金额有误', 'seed', now(), ?)",
                "tpbo240_" + bill, bill, status);
    }

    private void pointsState(RequestPostProcessor viewer, String bill, String expected) throws Exception {
        mockMvc.perform(get("/api/v1/loyalty/ticket-points-billing/bills").with(viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + bill + "')].settlement_state").value(expected));
    }
}
