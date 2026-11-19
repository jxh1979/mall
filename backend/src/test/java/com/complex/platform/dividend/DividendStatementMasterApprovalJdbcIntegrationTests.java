package com.complex.platform.dividend;

// 文件版本：V0.01

import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.EntityTaxFixture;
import com.complex.platform.support.InternalPayDayFixtures;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
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
 * 第十八批 BA 段第 314 条 314-R1（BSM:1794「有最高权限的管理者：审批分红单（提交单据的人不能审批自己提交的单）。」；PGM:666「……由最高权限审批，提交人不能
 * 审批自己的单据。」；BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」；第十八批裁决 71 最高权限什么都能看）：分红计算单的批准与驳回
 * 只认最高权限本人，其他持 finance.dividend.manage 的人 403 MASTER_APPROVAL_REQUIRED；最高权限在分红页看得到待审单并能批（含欠款抵扣大于 0 的单）；
 * 最高权限本人提交即审批通过。
 *
 * <p>夹具随测试事务回滚（照 DividendBarLoanJdbcIntegrationTests）：酒吧 2026-08 五类全标、经营主体各月有已生效税费登记、已确认分账单里单店门票直付 50,000 元；
 * 月度方案平台 80%、个人主理人 20%。请求按持股分红页的蛇形键走真实入口与授权链。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class DividendStatementMasterApprovalJdbcIntegrationTests {

    private static final LocalDate AUG_1 = LocalDate.of(2026, 8, 1);
    private static final LocalDate AUG_31 = LocalDate.of(2026, 8, 31);
    private static final long D = 5_000_000L;
    private static final String STATEMENTS = "/api/v1/finance/dividends/statements";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "dsm_p_" + s;
    private final String bill = "sb_dsm_" + s;
    private RequestPostProcessor finance;
    private RequestPostProcessor approver;

    @BeforeEach
    void seed() {
        InternalPayDayFixtures.seed(jdbc);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at)"
                + " VALUES(?,'complex','分红审批综合体','active',TIMESTAMPTZ '2026-05-01 00:00:00+08',now())", project);
        jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, 'confirmed', now(), now())", bill, project, AUG_1, AUG_31);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        finance = staffs.staff("usr_dsm_fin_" + s, "platform_admin", project, null, "finance.dividend.manage", "finance.dividend.view");
        approver = staffs.staff("usr_dsm_apv_" + s, "platform_admin", project, null, "finance.dividend.manage", "finance.dividend.view");
    }

    /**
     * ①②③ 平台管理员提交后，另一位持 finance.dividend.manage 的平台管理员批准、驳回都 403 MASTER_APPROVAL_REQUIRED，单据仍待审批、不挂应付；
     * 最高权限在分红页列表与详情看得到该单，批准 200 并挂出主理人分红应付。修复前非最高权限批准 200，断言红。
     */
    @Test
    void onlyMasterAuthorityApprovesAndMasterSeesPendingStatement() throws Exception {
        String bar = bar("a", false);
        String statement = draft(bar, "a");
        action(finance, statement, "submit", "a1", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"));

        action(approver, statement, "approve", "a2", null).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MASTER_APPROVAL_REQUIRED"));
        action(approver, statement, "reject", "a3", "数字有误").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MASTER_APPROVAL_REQUIRED"));
        assertThat(statusOf(statement)).isEqualTo("pending_review");
        assertThat(payables(statement)).isZero();

        String listed = mockMvc.perform(get(STATEMENTS).param("status", "pending_review").param("merchant_id", bar).with(master()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<List<String>>read(listed, "$.data.items[*].id")).contains(statement);
        mockMvc.perform(get(STATEMENTS + "/" + statement).with(master())).andExpect(status().isOk());

        action(master(), statement, "approve", "a4", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"))
                .andExpect(jsonPath("$.data.approved_by").value(SessionIssuer.MASTER_USER_ID));
        assertThat(payables(statement)).isEqualTo(1);
    }

    /** ④ 最高权限本人提交即审批通过：提交人与批准人都是最高权限，挂出应付，提交与批准各落一条留痕。修复前提交后停在待审批，断言红。 */
    @Test
    void masterSubmissionIsApprovedAtOnce() throws Exception {
        String statement = draft(bar("b", false), "b");
        action(master(), statement, "submit", "b1", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"))
                .andExpect(jsonPath("$.data.submitted_by").value(SessionIssuer.MASTER_USER_ID))
                .andExpect(jsonPath("$.data.approved_by").value(SessionIssuer.MASTER_USER_ID));
        assertThat(payables(statement)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT event_name FROM audit_events WHERE business_id = ? AND event_name IN ('dividend.submitted', 'dividend.approved')",
                String.class, statement)).containsExactlyInAnyOrder("dividend.submitted", "dividend.approved");
    }

    /**
     * ⑤ 欠款抵扣大于 0 的单：主理人欠该酒吧 5,000 元未清应收，出单冻结抵扣；平台管理员提交后最高权限批准 200，该应收按抵扣额结清。
     * 修复前最高权限按项目闸只到本人组织，结算应收 403 CROSS_PROJECT_FORBIDDEN，断言红。
     */
    @Test
    void masterApprovesStatementWithArrearsOffset() throws Exception {
        String bar = bar("c", true);
        String receivable = "arap_dsm_" + s;
        Timestamp ts = Timestamp.from(Instant.parse("2026-08-15T04:00:00Z"));
        jdbc.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id,
                        amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date, accounting_period,
                        status, created_at, updated_at, merchant_id, project_id)
                VALUES (?, ?, 'receivable', ?, 'manual', ?, 500000, 0, 500000, 'CNY', '2026-08-01', '2026-08',
                        'pending_collection', ?, ?, ?, ?)
                """, receivable, "AR-DSM-" + s, principal("c"), receivable, ts, ts, bar, project);
        String statement = draft(bar, "c");
        assertThat(jdbc.queryForObject("SELECT arrears_offset_cent FROM dividend_statements WHERE id = ?", Long.class, statement))
                .isEqualTo(500_000L);
        action(finance, statement, "submit", "c1", null).andExpect(status().isOk());

        action(master(), statement, "approve", "c2", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"));
        assertThat(jdbc.queryForObject("SELECT unpaid_amount_cent FROM ar_ap_items WHERE id = ?", Long.class, receivable)).isZero();
    }

    // ---------- helpers ----------

    private String principal(String tag) {
        return "usr_dsm_principal_" + tag + "_" + s;
    }

    /** 酒吧：2026-08 五类全标、各月税费已登记、分账单里门票直付 50,000 元，月度方案平台 80%、个人主理人 20%（arrears＝开欠款抵扣）。 */
    private String bar(String tag, boolean arrears) {
        String bar = "dsm_bar_" + tag + "_" + s;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at)"
                + " VALUES(?,?,'bar','active',?,?,now())", bar, "分红审批酒吧" + tag + s, project, Timestamp.from(Instant.parse("2026-04-30T16:00:00Z")));
        markRangeFinal(jdbc, "bar", bar, AUG_1, AUG_31);
        jdbc.update("INSERT INTO settlement_items (id, bill_id, merchant_id, distribution_method, calculated_amount_cent, final_amount_cent,"
                + " status) VALUES (?, ?, ?, 'exclusive_full', ?, ?, 'draft')", "si_dsm_" + tag + "_" + s, bill, bar, D, D);
        EntityTaxFixture.taxDeclaredForAllMonths(jdbc, bar);
        jdbc.update("INSERT INTO dividend_plans (id, merchant_id, principal_user_id, platform_share_bps, period_type, revenue_domains,"
                + " include_inventory_cost, status, payout_pay_slot, effective_from, reason, period_min_months, period_max_months, principal_type,"
                + " arrears_offset_enabled)"
                + " VALUES (?, ?, ?, 8000, 'monthly', 'ticket', TRUE, 'active', 1, DATE '2000-01-01', '工商登记持股比例', 1, 12, 'individual', ?)",
                "divp_dsm_" + tag + "_" + s, bar, principal(tag), arrears);
        return bar;
    }

    /** 分红页期间作业「正式单」后由财务登记依法可分配金额（按核定额重算出新草稿），返回新草稿单号。 */
    private String draft(String bar, String tag) throws Exception {
        String computed = JsonPath.read(mockMvc.perform(post(STATEMENTS + "/compute").with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dsm-c-" + tag + "-" + s + "\",\"period_label\":\"2026-08\",\"kind\":\"official\","
                                + "\"merchant_id\":\"" + bar + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.computed[0].id");
        return JsonPath.read(mockMvc.perform(post(STATEMENTS + "/" + computed + "/legal-confirmation").with(finance)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dsm-l-" + tag + "-" + s + "\",\"legal_distributable_cent\":" + D
                                + ",\"tax_accounting_ref\":\"财税核算报告 TA-2026-08\",\"resolution_ref\":\"股东会分配决议 R-2026-08\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private ResultActions action(RequestPostProcessor who, String statement, String verb, String tag, String note) throws Exception {
        return mockMvc.perform(post(STATEMENTS + "/" + statement + "/" + verb).with(who).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"dsm-" + verb + "-" + tag + "-" + s + "\"" + (note == null ? "" : ",\"note\":\"" + note + "\"") + "}"));
    }

    private String statusOf(String statement) {
        return jdbc.queryForObject("SELECT status FROM dividend_statements WHERE id = ?", String.class, statement);
    }

    private int payables(String statement) {
        return jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_type = 'dividend_payout' AND source_id = ?",
                Integer.class, statement);
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
