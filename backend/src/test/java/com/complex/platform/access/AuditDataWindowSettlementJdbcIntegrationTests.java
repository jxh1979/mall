package com.complex.platform.access;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.PointsRecoveryWaiverService;
import com.complex.platform.customer.TicketRewardConfig;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 CN 段第 372 条第 3 段 372-S3-R2～R5（PGM:682「外部审计/监管只能查看划定的范围与时间段」；第十八批裁决 77「开外审权限必须划定数据起止日期，
 * 各清单、明细、导出只看得到这段时间发生的记录；没有发生日期的资料照常显示」）：通票结算流水与月度结算单头、开票清单与其导出、工资银行付款明细、
 * 积分追回明细按外审数据时段承载。外审账号（外部人员、本人项目 p1、时段 2026-01-01～2026-12-31）对照同权限、不划时段的 p1 项目员工。
 * 真库＋真实接口＋真实角色授权链，夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AuditDataWindowSettlementJdbcIntegrationTests {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final String[] PERMISSIONS = {"finance.settlement.view", "finance.payroll.view", "finance.points_recovery.view",
            "settlement.invoice.view", "settlement.invoice.export"};

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;

    private final String s = String.valueOf(System.nanoTime());
    private final String p1 = "adws_p1_" + s;
    private final String p2 = "adws_p2_" + s;
    private final String m1 = "adws_m1_" + s;
    private final String m2 = "adws_m2_" + s;
    private final String auditor = "usr_adws_auditor_" + s;
    private final String control = "usr_adws_ctl_" + s;
    private StaffRequestFixture staffs;
    private RequestPostProcessor auditorRequest;
    private RequestPostProcessor controlRequest;

    @BeforeEach
    void setUp() throws Exception {
        for (String project : List.of(p1, p2)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','外审时段结算综合体','active',now(),now())",
                    project);
        }
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, '外审时段结算商户', 'amusement', ?, 'active', now(), now())", m1, p1);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, '外审时段别项目商户', 'amusement', ?, 'active', now(), now())", m2, p2);
        staffs = new StaffRequestFixture(jdbc);
        auditorRequest = staffs.staff(auditor, "outsourced_vendor", p1, null, PERMISSIONS);
        controlRequest = staffs.staff(control, "staff", p1, null, PERMISSIONS);
        setWindow("2026-01-01", "2026-12-31");
    }

    // ---------- 372-S3-R2 通票结算流水与月度结算单头 ----------

    @Test
    void settlementTransactionsKeepOnlyBusinessTimeInsideWindowWithMatchingTotal() throws Exception {
        transaction("before", "2025-12-31T23:30:00+08:00");
        String inside = transaction("inside", "2026-06-01T10:00:00+08:00");
        transaction("after", "2027-01-01T00:00:00+08:00");
        String url = "/api/v1/settlement/transactions?projectId=" + p1;
        String body = read(url, auditorRequest);
        assertThat(ids(body, "$.data.items[*].id")).containsExactly(inside);
        assertThat(total(body)).isEqualTo(1L);
        String all = read(url, controlRequest);
        assertThat(ids(all, "$.data.items[*].id")).hasSize(3);
        assertThat(total(all)).isEqualTo(3L);
    }

    @Test
    void settlementBillsKeepOnlyPeriodsWhollyInsideWindowAndMonthNarrowsFurther() throws Exception {
        bill("2026-01-01", "2026-01-31");
        String february = bill("2026-02-01", "2026-02-28");
        bill("2027-01-01", "2027-01-31");
        setWindow("2026-01-15", "2026-12-31");
        String url = "/api/v1/settlement/bills?projectId=" + p1;
        assertThat(ids(read(url, auditorRequest), "$.data[*].id")).containsExactly(february);
        assertThat(ids(read(url + "&month=2027-01", auditorRequest), "$.data[*].id")).isEmpty();
        assertThat(ids(read(url, controlRequest), "$.data[*].id")).hasSize(3);
    }

    // ---------- 372-S3-R3 开票清单与其导出 ----------

    @Test
    void invoiceItemsKeepOnlyInvoicePeriodsWhollyInsideWindowWithMatchingTotal() throws Exception {
        invoiceItem("2025-12-01", "2025-12-31");
        invoiceItem("2026-01-01", "2026-01-31");
        String inside = invoiceItem("2026-06-01", "2026-06-30");
        invoiceItem("2027-01-01", "2027-01-31");
        setWindow("2026-01-15", "2026-12-31");
        String url = "/api/v1/settlement/invoice-items?projectId=" + p1;
        String body = read(url, auditorRequest);
        assertThat(ids(body, "$.data.items[*].id")).containsExactly(inside);
        assertThat(total(body)).isEqualTo(1L);
        assertThat(total(read(url, controlRequest))).isEqualTo(4L);
    }

    @Test
    void invoiceExportRecordsWindowAndCountsOnlyItemsInsideIt() throws Exception {
        invoiceItem("2025-12-01", "2025-12-31");
        invoiceItem("2026-01-01", "2026-01-31");
        invoiceItem("2026-06-01", "2026-06-30");
        setWindow("2026-01-15", "2026-12-31");
        String device = "WBADWS" + s.substring(s.length() - 10);
        jdbc.update("INSERT INTO browser_devices (device_no, first_user_id) VALUES (?, ?)", device, auditor);
        mockMvc.perform(get("/api/v1/settlement/invoice-items/export").param("projectId", p1).with(auditorRequest)
                        .cookie(new Cookie("complex_device_no", device)))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT filters->>'data_window' FROM export_files WHERE exporter_id = ?"
                + " AND export_type = 'settlement_invoice_items'", String.class, auditor)).isEqualTo("2026-01-15～2026-12-31");
        assertThat(jdbc.queryForObject("SELECT metadata->>'row_count' FROM audit_events WHERE event_name = 'settlement.invoice.exported'"
                + " AND actor_id = ?", String.class, auditor)).isEqualTo("1");
    }

    // ---------- 372-S3-R4 工资银行付款明细 ----------

    @Test
    void payrollPaymentContextNeedsWholeWageMonthInsideWindow() throws Exception {
        String december = wage("2025-12");
        String june = wage("2026-06");
        attempts(december, auditorRequest)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("OUT_OF_AUDIT_DATA_WINDOW"));
        attempts(june, auditorRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attempts").isArray());
        attempts(december, controlRequest).andExpect(status().isOk());

        setWindow("2026-01-15", "2026-12-31");
        String january = wage("2026-01");
        attempts(january, auditorRequest)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("OUT_OF_AUDIT_DATA_WINDOW"));

        setWindow("2026-01-01", "2026-06-15");
        attempts(june, auditorRequest)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("OUT_OF_AUDIT_DATA_WINDOW"));
    }

    // ---------- 372-S3-R5 积分追回明细 ----------

    @Test
    void pointsRecoveryDetailListsOnlyRecoveriesAndWaiversInsideWindow() throws Exception {
        String member = member();
        String config = waiverConfig();
        String growthRule = growthRule();
        String before = recovery(member, m1, p1, growthRule, "2025-12-31T10:00:00+08:00", 30);
        String inside = recovery(member, m1, p1, growthRule, "2026-06-01T10:00:00+08:00", 40);
        waiver(member, config, before, "2025-12-31T12:00:00+08:00");
        String url = "/api/v1/finance/points-recoveries/" + member;
        String body = read(url, auditorRequest);
        assertThat(ids(body, "$.data.lines[*].reward_id")).containsExactly(inside);
        assertThat(((Number) JsonPath.read(body, "$.data.pending_points")).longValue()).isEqualTo(40L);
        assertThat(ids(body, "$.data.waivers[*].id")).isEmpty();
        String all = read(url, controlRequest);
        assertThat(ids(all, "$.data.lines[*].reward_id")).containsExactlyInAnyOrder(before, inside);
        assertThat(ids(all, "$.data.waivers[*].id")).hasSize(1);
    }

    @Test
    void pointsRecoveryDetailIsEmptyWithoutErrorWhenEveryRecordIsOutsideWindow() throws Exception {
        String member = member();
        waiverConfig();
        recovery(member, m1, p1, growthRule(), "2025-12-31T10:00:00+08:00", 30);
        mockMvc.perform(get("/api/v1/finance/points-recoveries/" + member).with(auditorRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lines").isArray())
                .andExpect(jsonPath("$.data.lines").isEmpty())
                .andExpect(jsonPath("$.data.waivers").isArray())
                .andExpect(jsonPath("$.data.waivers").isEmpty())
                .andExpect(jsonPath("$.data.pending_points").value(0));
    }

    @Test
    void pointsRecoveryDetailChecksEverySourceScopeBeforeWindow() throws Exception {
        String member = member();
        waiverConfig();
        String growthRule = growthRule();
        recovery(member, m1, p1, growthRule, "2026-06-01T10:00:00+08:00", 40);
        recovery(member, m2, p2, growthRule, "2025-06-01T10:00:00+08:00", 40);
        mockMvc.perform(get("/api/v1/finance/points-recoveries/" + member).with(auditorRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
    }

    // ---------- helpers ----------

    private void setWindow(String from, String to) throws Exception {
        mockMvc.perform(put("/api/v1/access/users/" + auditor + "/audit-data-window").with(master())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"adws-" + UUID.randomUUID() + "\",\"data_from\":\"" + from + "\",\"data_to\":\"" + to + "\"}"))
                .andExpect(status().isOk());
    }

    private String read(String url, RequestPostProcessor who) throws Exception {
        return mockMvc.perform(get(url).with(who)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private ResultActions attempts(String wage, RequestPostProcessor who) throws Exception {
        return mockMvc.perform(get("/api/v1/finance/payroll/" + wage + "/payment-attempts").with(who));
    }

    private static List<String> ids(String body, String path) {
        return JsonPath.read(body, path);
    }

    private static long total(String body) {
        return ((Number) JsonPath.read(body, "$.data.total")).longValue();
    }

    /** p1 的通票核销结算流水；建立时刻取现在（在时段内），发生时间看核销业务时刻 business_at。 */
    private String transaction(String tag, String businessAt) {
        String id = "st_adws_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id, entitlement_type, verified_count,
                    total_times, num_people, allocated_revenue_cent, verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id,
                    project_id, settlement_status, entitlement_valid_to, business_at, created_at, updated_at)
                VALUES (?, ?, 'verification_settled', ?, 'ticket', 1, 1, 1, 100, 100, 0, 'merchant', ?, ?, 'draft', now(),
                        CAST(? AS timestamptz), now(), now())
                """, id, id, id, m1, p1, businessAt);
        return id;
    }

    /** p1 的月度结算单（按账期起止建）。 */
    private String bill(String start, String end) {
        String id = "sb_adws_" + start + "_" + s;
        jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status, created_at, updated_at)"
                + " VALUES (?, ?, CAST(? AS date), CAST(? AS date), 'confirmed', now(), now())", id, p1, start, end);
        return id;
    }

    /** p1 的开票清单条目，各挂一张本例新建的结算单（来源四选一、未红冲同来源唯一）。 */
    private String invoiceItem(String start, String end) {
        String id = "sii_adws_" + start + "_" + s;
        jdbc.update("""
                INSERT INTO settlement_invoice_items (id, bill_id, project_id, period_start, period_end, income_type, invoice_item_name,
                    amount_cent, status)
                VALUES (?, ?, ?, CAST(? AS date), CAST(? AS date), 'ticket', '综合体验服务费', 10000, 'pending')
                """, id, bill(start, end), p1, start, end);
        return id;
    }

    /** p1 员工某月的已定格工资单（已批准方案、日历与期间事实，发薪主体即 p1）；每张用各自的员工账号。 */
    private String wage(String month) {
        YearMonth ym = YearMonth.parse(month);
        String tag = month.replace("-", "") + "_" + s;
        String employee = "usr_adws_emp_" + tag;
        staffs.staff(employee, "staff", p1, null);
        String scheme = "adws_scheme_" + tag;
        String calendar = "adws_cal_" + tag;
        String facts = "adws_facts_" + tag;
        String wage = "adws_wage_" + tag;
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        LocalDate due = ym.plusMonths(1).atDay(5);
        jdbc.update("""
                INSERT INTO staff_profiles(id,project_id,user_id,name,company_id,registration_status,status,employment_status,created_at,updated_at)
                VALUES(?,?,?,'外审时段工资员工',?,'approved','active','employed',now(),now())
                """, "adws_profile_" + tag, p1, employee, p1);
        jdbc.update("""
                INSERT INTO staff_payroll_scheme_versions(id,user_id,project_id,payer_company_id,version,effective_from,position_code,
                 components,worktime_terms,payment_basis,reason,status,created_by,approved_at)
                VALUES(?,?,?,?,'V0.01',?,'test_position','[]','{}','已核工资支付日期','测试已批准来源','approved',?,now())
                """, scheme, employee, p1, p1, start, control);
        jdbc.update("""
                INSERT INTO staff_payroll_calendar_versions(id,project_id,version,start_date,end_date,days,source_basis,reason,status,created_by,approved_at)
                VALUES(?,?,'V0.01',?,?,'[]','测试日历','已批准来源','approved',?,now())
                """, calendar, p1, start, end, control);
        jdbc.update("""
                INSERT INTO staff_payroll_period_facts(id,user_id,month,project_id,payer_company_id,scheme_version_id,calendar_version_id,
                 component_facts,cycle_start,cycle_end,cycle_standard_minutes,agreed_due_date,reason,status,created_by,approved_at)
                VALUES(?,?,?,?,?,?,?,'[]',?,?,0,?,'测试已批准期间','approved',?,now())
                """, facts, employee, month, p1, p1, scheme, calendar, start, end, due, control);
        jdbc.update("""
                INSERT INTO staff_payroll_statements(id,user_id,month,base_cent,performance_cent,penalty_cent,total_cent,status,
                  project_id,payer_company_id,scheme_version_id,facts_id,calculation_snapshot,payment_due_date,payment_due_basis,statement_kind)
                VALUES(?,?,?,500000,80000,100000,480000,'closed',?,?,?,?,?::jsonb,?,?::jsonb,'monthly')
                """, wage, employee, month, p1, p1, scheme, facts, "{\"gross_cent\":480000}", due, "{\"approved_facts\":\"" + facts + "\"}");
        return wage;
    }

    private String member() {
        String k = UUID.randomUUID().toString().replace("-", "");
        String id = "c_adws_" + k;
        Instant t = Instant.parse("2025-01-01T00:00:00Z");
        customers.saveCustomer(new CustomerRecord(id, "137" + String.format("%08d", Math.floorMod(k.hashCode(), 100_000_000)), "***", "active",
                null, t, t, t));
        return id;
    }

    /** 已批准、已公告并已生效的积分小额豁免线（明细读取取最新生效版）。 */
    private String waiverConfig() throws Exception {
        String key = UUID.randomUUID().toString().replace("-", "");
        String id = "trc_" + key;
        String version = "V" + Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 1_000_000_000_000L) + ".01";
        Instant effective = Instant.now().minusSeconds(120);
        jdbc.update("INSERT INTO ticket_reward_config_versions(id,kind,version,payload,effective_at,published_at,status,created_by,created_at,approved_at)"
                        + " VALUES(?,'recovery_waiver',?,?::jsonb,?,?,'approved','test-reviewer',?,?)",
                id, version, json.writeValueAsString(new TicketRewardConfig.Values(100L, 0L, 200L, null, null, null, null, null, null, null, null,
                        null, null, List.of())),
                Timestamp.from(effective), Timestamp.from(effective.minusSeconds(60)), Timestamp.from(effective.minusSeconds(120)),
                Timestamp.from(effective.minusSeconds(60)));
        return id;
    }

    /** 商户消费成长值规则（奖励只记成长值、积分为 0）。 */
    private String growthRule() {
        String key = UUID.randomUUID().toString().replace("-", "");
        String id = "trc_" + key;
        jdbc.update("INSERT INTO ticket_reward_config_versions(id,kind,business_format,version,payload,effective_at,status,created_by,created_at,approved_at)"
                        + " VALUES(?,'merchant_growth','bar',?,'{\"fixedValue\":100}'::jsonb,?,'approved','test',now(),now())",
                id, "V" + Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 1_000_000_000_000L) + ".01",
                Timestamp.from(Instant.parse("2001-01-01T00:00:00Z").plus(Math.floorMod(key.hashCode(), 400_000), ChronoUnit.MINUTES)));
        return id;
    }

    /** 一笔商户消费奖励（带项目）及其待追回记录，两者都在 createdAt 建立；返回奖励号（即明细 lines 的 reward_id）。 */
    private String recovery(String member, String merchant, String project, String growthRule, String createdAt, long points) {
        String reward = "mcr_adws_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.update("""
                INSERT INTO merchant_consumption_rewards(id,source_type,source_id,customer_id,merchant_id,project_id,business_format,effective_amount_cent,
                    growth_rule_id,level_multiplier_bp,birthday_multiplier_bp,paid_at,base_points,points,growth,completed_at,status,created_at)
                VALUES(?,'service_order',?,?,?,?,'bar',10000,?,10000,10000,CAST(? AS timestamptz),0,0,100,CAST(? AS timestamptz),'confirmed',
                    CAST(? AS timestamptz))
                """, reward, reward, member, merchant, project, growthRule, createdAt, createdAt, createdAt);
        jdbc.update("""
                INSERT INTO customer_points_recoveries(merchant_reward_id,customer_id,original_points,remaining_points,status,created_at,updated_at)
                VALUES(?,?,?,?,'pending',CAST(? AS timestamptz),CAST(? AS timestamptz))
                """, reward, member, points, points, createdAt, createdAt);
        return reward;
    }

    /** 一条豁免原件（lines 为那次豁免的快照，按服务 ObjectMapper 序列化）。 */
    private void waiver(String member, String config, String rewardId, String createdAt) throws Exception {
        Instant at = OffsetDateTime.parse(createdAt).toInstant();
        String lines = json.writeValueAsString(List.of(new PointsRecoveryWaiverService.Line(rewardId, p1, m1, 30, 30, "pending", at, at)));
        jdbc.update("INSERT INTO customer_points_recovery_waivers(id,customer_id,config_version_id,threshold,waived_points,actor_id,reason,lines,created_at)"
                        + " VALUES(?,?,?,100,30,?,'外审时段豁免',?::jsonb,CAST(? AS timestamptz))",
                "prw_adws_" + UUID.randomUUID().toString().replace("-", ""), member, config, control, lines, createdAt);
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
