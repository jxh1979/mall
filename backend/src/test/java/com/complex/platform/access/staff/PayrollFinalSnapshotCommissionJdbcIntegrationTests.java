package com.complex.platform.access.staff;

// 文件版本：V0.01

import static com.complex.platform.access.staff.StaffPayrollModels.*;
import static com.complex.platform.support.DailyCloseFixture.closeWithMarks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 V 段第 159 条 159-R7：工资计算只收所属营业日相关类别已标最终的提成。依据：ENUM:190「非最终快照的数据一律不得进入结算、分红、正式提成计算和对账关账」；
 * BSM:1500「正式提成使用该项计算所依赖的已最终确认数据，再统一进入工资单月结」；决策台账 §25 Q32「正式提成仅用该项计算依赖的最终确认数据，再进入原工资月结。
 * 不要求无关五类数据全最终，不新增工资发放日、退款重算、月结重开或税务政策。」；第十八批裁决 35「酒吧逐店逐日查；游乐等按综合体逐日查」。
 *
 * <p>夹具随测试事务回滚：综合体与其下酒吧都在 2025-01-01 建档，工资月 2025-07。财务 BP 持 finance.daily_close.lock 与 finance.daily_close.manage、
 * 负责范围是该综合体，日结生成与标记走真实接口（蛇形键）与真实授权链。人事持 staff.payroll.manage（真库角色、负责范围同上），工资计算与定格按前端
 * 请求形状（蛇形键）经真实接口 POST /api/v1/staff/payroll/calculations 与 /settlements、走真实授权链（控制席补：9d8af4dc 上
 * StaffPayrollController.call 经代理读 config.audit 为空，/api/v1/staff/payroll 下接口一律 500，已改为构造注入审计服务）。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class PayrollFinalSnapshotCommissionJdbcIntegrationTests {

    private static final String MONTH = "2025-07";
    private static final LocalDate D15 = LocalDate.of(2025, 7, 15);
    private static final LocalDate D16 = LocalDate.of(2025, 7, 16);

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_pfs_" + s;
    private final String bar = "mer_pfs_bar_" + s;
    private final String barName = "提成快照酒吧" + s;
    private final String projectName = "提成快照综合体" + s;
    private final String user = "usr_pfs_emp_" + s;
    private final String hrId = "usr_pfs_hr_" + s;
    private String facts;
    private RequestPostProcessor financeBp;
    private RequestPostProcessor hr;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', ?, 'active', TIMESTAMPTZ '2025-01-01 00:00:00+08', now())", project, projectName);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', ?, 'active', TIMESTAMPTZ '2025-01-01 00:00:00+08', now())", bar, barName, project);
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'staff', ?, NULL, 'active', now())", user, user, project);
        jdbc.update("INSERT INTO staff_profiles (id, project_id, user_id, name, company_id, registration_status, status, employment_status,"
                + " created_at, updated_at) VALUES (?, ?, ?, '提成快照员工', ?, 'approved', 'active', 'employed', now(), now())",
                "prof_pfs_" + s, project, user, project);
        StaffRequestFixture fixture = new StaffRequestFixture(jdbc);
        hr = fixture.staff(hrId, "staff", project, null, "staff.payroll.manage");
        financeBp = fixture.staff("usr_pfs_bp_" + s, "staff", project, null, "finance.daily_close.lock", "finance.daily_close.manage");
        facts = PayrollApprovedFactsFixture.create(jdbc, json, user, project, hrId, MONTH, 100000, 0).facts();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // 修复前已结算账单的提成不看最终标记直接进工资：第一条断言（pending 有 BAR_COMMISSION_PENDING）即红。
    @Test
    void barCommissionWaitsUntilRefundsAreFinalThenSupplementPaysIt() throws Exception {
        String commission = barCommission(4500);
        String close = JsonPath.read(mockMvc.perform(post("/api/v1/finance/daily-closes").with(financeBp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"pfs-gen-" + s + "\",\"business_date\":\"" + D15
                                + "\",\"scope_type\":\"bar\",\"scope_id\":\"" + bar + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        for (String category : List.of("orders_verifications", "production_handover", "attendance", "anomaly_resolutions")) {
            finalMark(close, category);
        }

        CalculationDraft monthly = calculate("monthly");
        var snapshot = snapshot(monthly);
        assertThat(snapshot.pending()).anyMatch(p -> "BAR_COMMISSION_PENDING".equals(p.code()) && commission.equals(p.sourceId())
                && p.message().contains("尚未标记为最终") && p.message().contains(D15 + " " + barName + "：当日退款与冲正记录"));
        assertThat(snapshot.lines()).noneMatch(l -> "bar_commission".equals(l.sourceType()));
        String original = settle(monthly);
        assertThat(jdbc.queryForObject("SELECT commission_cent FROM staff_payroll_statements WHERE id = ?", Long.class, original)).isZero();
        assertThat(pendingStatus("BAR_COMMISSION_PENDING", commission)).isEqualTo("pending");

        finalMark(close, "refunds_reversals");
        CalculationDraft supplement = calculate("supplemental");
        assertThat(snapshot(supplement).lines()).anyMatch(l -> "bar_commission".equals(l.sourceType()) && commission.equals(l.sourceId())
                && l.amountCent() == 4500);
        String paid = settle(supplement);
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM staff_payroll_statement_lines WHERE statement_id = ? AND source_type = 'bar_commission'"
                + " AND source_id = ?", Long.class, paid, commission)).isEqualTo(4500L);
        assertThat(pendingStatus("BAR_COMMISSION_PENDING", commission)).isEqualTo("resolved");
    }

    // Q32「不要求无关五类数据全最终」：只缺当日考勤记录时提成照常进工资（修复前后都绿，防闸收得过宽）。
    @Test
    void barCommissionDoesNotWaitForUnrelatedAttendance() throws Exception {
        String commission = barCommission(3000);
        closeWithMarks(jdbc, "bar", bar, D15, List.of("orders_verifications", "refunds_reversals", "production_handover",
                "anomaly_resolutions"));
        var snapshot = snapshot(calculate("monthly"));
        assertThat(snapshot.lines()).anyMatch(l -> "bar_commission".equals(l.sourceType()) && commission.equals(l.sourceId()));
        assertThat(snapshot.pending()).noneMatch(p -> "BAR_COMMISSION_PENDING".equals(p.code()));
    }

    // 不选酒吧的服务单按综合体营业日判订单与核销（裁决 35）；修复前份额不看最终标记直接进工资：第一条断言即红。
    @Test
    void serviceShareWithoutBarWaitsForComplexOrdersFinalOnly() throws Exception {
        String waiting = serviceShare(D15, 20000);
        String ready = serviceShare(D16, 30000);
        closeWithMarks(jdbc, "project", project, D15, List.of("refunds_reversals", "production_handover", "attendance",
                "anomaly_resolutions"));
        closeWithMarks(jdbc, "project", project, D16, List.of("orders_verifications", "production_handover", "attendance",
                "anomaly_resolutions"));

        CalculationDraft monthly = calculate("monthly");
        var snapshot = snapshot(monthly);
        assertThat(snapshot.pending()).anyMatch(p -> "SERVICE_COMMISSION_PENDING".equals(p.code()) && waiting.equals(p.sourceId())
                && p.message().contains(D15 + " " + projectName + "：当日订单与核销记录"));
        assertThat(snapshot.lines()).noneMatch(l -> waiting.equals(l.sourceId()));
        assertThat(snapshot.lines()).anyMatch(l -> "staff_commission_ledger".equals(l.sourceType()) && ready.equals(l.sourceId())
                && l.amountCent() == 30000);
        settle(monthly);
        assertThat(pendingStatus("SERVICE_COMMISSION_PENDING", waiting)).isEqualTo("pending");

        closeWithMarks(jdbc, "project", project, D15, List.of("orders_verifications"));
        CalculationDraft supplement = calculate("supplemental");
        assertThat(snapshot(supplement).lines()).anyMatch(l -> "staff_commission_ledger".equals(l.sourceType()) && waiting.equals(l.sourceId())
                && l.amountCent() == 20000);
        settle(supplement);
        assertThat(pendingStatus("SERVICE_COMMISSION_PENDING", waiting)).isEqualTo("resolved");
    }

    // 控制席补（并入 159-R7）：修复前 StaffPayrollController.call 经代理读 config.audit 为空，只读接口同样 500。
    @Test
    void hrListsCalculationsThroughRealEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/staff/payroll/calculations").param("user_id", user).param("month", MONTH).with(hr))
                .andExpect(status().isOk());
    }

    /** 人事（真库角色只持 staff.payroll.manage）按前端请求形状（StaffPayrollBasisPanel 经 adminClient 蛇形化）经真实接口算工资原件。 */
    private CalculationDraft calculate(String kind) throws Exception {
        String body = "{\"idempotency_key\":\"" + key() + "\",\"user_id\":\"" + user + "\",\"month\":\"" + MONTH
                + "\",\"facts_id\":\"" + facts + "\",\"statement_kind\":\"" + kind + "\",\"origin_event_id\":null}";
        String response = mockMvc.perform(post("/api/v1/staff/payroll/calculations").with(hr).contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Map<String, Object> data = JsonPath.read(response, "$.data");
        return json.copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).convertValue(data, CalculationDraft.class);
    }

    /** 本用例批准 0 分税额后，人事按前端请求形状（OpenAPI StaffPayrollSettleRequest）经真实接口定格；返回工资单编号。 */
    private String settle(CalculationDraft calc) throws Exception {
        StaffTestActors.actor(hrId, "staff", project, null);
        String tax = PayrollApprovedCloseFixture.approvedZeroTax(jdbc, json, calc);
        String body = "{\"facts_id\":\"" + facts + "\",\"statement_kind\":\"" + calc.statementKind() + "\",\"calculation_id\":\""
                + calc.id() + "\",\"tax_assessment_id\":\"" + tax + "\",\"idempotency_key\":\"" + key() + "\",\"user_id\":\"" + user
                + "\",\"month\":\"" + MONTH + "\",\"origin_event_id\":null}";
        return JsonPath.read(mockMvc.perform(post("/api/v1/staff/payroll/settlements").with(hr).contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.statement.id");
    }

    private StaffPayrollService.Snapshot snapshot(CalculationDraft calc) {
        return json.convertValue(calc.calculationSnapshot(), StaffPayrollService.Snapshot.class);
    }

    private String pendingStatus(String code, String sourceId) {
        return jdbc.queryForObject("SELECT status FROM staff_payroll_pending_sources WHERE user_id = ? AND source_type = ? AND source_id = ?",
                String.class, user, code, sourceId);
    }

    private void finalMark(String dailyCloseId, String category) throws Exception {
        mockMvc.perform(post("/api/v1/finance/daily-closes/" + dailyCloseId + "/final-marks").with(financeBp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"pfs-" + category + "-" + key() + "\",\"category\":\"" + category + "\"}"))
                .andExpect(status().isOk());
    }

    /** 本员工为销售归属人、已付清并确认结算的酒吧账单及其提成计提；下单时点 7-15 北京 12:00（该酒吧 7-15 营业日，ENUM:187）。 */
    private String barCommission(long amount) {
        String table = "tbl_pfs_" + key(), order = "ord_pfs_" + key(), bill = "bill_pfs_" + key(), id = "cm_pfs_" + key();
        String at = D15 + "T12:00:00+08:00";
        jdbc.update("INSERT INTO bar_tables (id, bar_id, table_code, table_name, table_type, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, '提成快照卡座', 'card_seat', 'active', now(), now())", table, bar, table);
        jdbc.update("INSERT INTO bar_orders (id, bar_id, table_id, order_source, status, total_amount_cent, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'staff', 'billed', 30000, ?::timestamptz, ?::timestamptz)", order, bar, table, at, at);
        jdbc.update("INSERT INTO bar_bills (id, bill_no, order_id, bar_id, merchant_id, table_id, subtotal_amount_cent, payable_amount_cent,"
                + " paid_amount_cent, payment_status, settlement_status, sales_staff_id, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, 30000, 30000, 30000, 'paid', 'settled', ?, now(), now())", bill, bill, order, bar, bar, table, user);
        jdbc.update("INSERT INTO bar_commission_calculations (id, bill_id, sales_staff_id, sales_role_type, commission_base_cent,"
                + " commission_rate_bp, commission_amount_cent, status, created_at) VALUES (?, ?, ?, 'sales', 30000, 1500, ?, 'calculated',"
                + " ?::timestamptz)", id, bill, user, amount, at);
        return id;
    }

    /** 不选酒吧的已完成服务单（属该综合体，day 北京 12:00 完成）及其份额台账行；返回台账行编号。 */
    private String serviceShare(LocalDate day, long share) {
        String order = "svo_pfs_" + key(), ledger = "cml_pfs_" + key();
        String at = day + "T12:00:00+08:00";
        jdbc.update("INSERT INTO service_orders (id, catalog_item_id, item_name, price_cent, performer_user_id, status, project_id,"
                + " paid_at, mgmt_fee_cent, dancer_share_cent, created_at, updated_at, completed_at)"
                + " VALUES (?, ?, '提成快照服务', ?, ?, 'completed', ?, ?::timestamptz, 0, ?, ?::timestamptz, ?::timestamptz, ?::timestamptz)",
                order, "cat_pfs_" + s, share, user, project, at, share, at, at, at);
        jdbc.update("INSERT INTO staff_commission_ledger (id, user_id, month, source_type, source_id, amount_cent, note, created_at, project_id)"
                + " VALUES (?, ?, ?, 'service_order', ?, ?, '提成快照服务', ?::timestamptz, ?)", ledger, user, MONTH, order, share, at, project);
        return ledger;
    }

    private static String key() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
