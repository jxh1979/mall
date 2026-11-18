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
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.support.EntityTaxFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AX 段第 271 条 271-R1 全平台统一分红代扣个税税率真库回归。依据：BSM:1776「其分得的款项按「股息红利」计税，由发放的经营主体代扣个人所得税
 * （安全阈值，全平台统一可配，调整须经财务、税务、法务三审并由顶层平台公司最终确认，出厂默认 20%，须同时配置上下限），发放时在单据上写清代扣了多少」；
 * BSM:521「每一笔单据在生成时就把所用配置版本固化在单上」「超出该区间的配置一律拒绝生效，未填写上下限的参数不得上线」；DEC-R8-82「调整须走最高一级审批与
 * 三审留痕」。
 *
 * <p>夹具随测试事务回滚：酒吧 2026-08 五类全标、经营主体各月有已生效税费登记、已确认分账单里单店门票直付 10,000.06 元；方案版本自 2000-01-01 起，平台 60%，
 * 个人股东「股东乙」10%、公司「丙公司」5%，个人主理人 25%（方案上不再有税率）。财务、税务、法务三条线起点与审批人就位。请求按持股分红页与主管复核页的
 * 蛇形键走真实入口与授权链。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class DividendWithholdingTaxRateJdbcIntegrationTests {

    private static final String RATES = "/api/v1/finance/dividends/tax-rates";
    private static final LocalDate AUG_1 = LocalDate.of(2026, 8, 1);
    private static final LocalDate AUG_31 = LocalDate.of(2026, 8, 31);
    private static final long D = 1_000_006L;
    private static final long PRINCIPAL = MoneyRounding.bpsOf(D, 2500);
    private static final long B_GROSS = MoneyRounding.bpsOf(D, 1000);

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BusinessDayResolver businessDays;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "dwt_p_" + s;
    private final String bar = "dwt_bar_" + s;
    private final String financeApprover = "usr_dwt_fr_" + s;
    private final String taxApprover = "usr_dwt_tr_" + s;
    private final String legalApprover = "usr_dwt_lr_" + s;
    private RequestPostProcessor finance;
    private RequestPostProcessor financeReviewer;
    private RequestPostProcessor taxReviewer;
    private RequestPostProcessor legalReviewer;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at)"
                + " VALUES(?,'complex','分红代扣税率综合体','active',TIMESTAMPTZ '2026-05-01 00:00:00+08',now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at)"
                + " VALUES(?,?,'bar','active',?,?,now())", bar, "分红代扣税率酒吧" + s, project, Timestamp.from(Instant.parse("2026-04-30T16:00:00Z")));
        markRangeFinal(jdbc, "bar", bar, AUG_1, AUG_31);
        jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, 'confirmed', now(), now())", "sb_dwt_" + s, project, AUG_1, AUG_31);
        jdbc.update("INSERT INTO settlement_items (id, bill_id, merchant_id, distribution_method, calculated_amount_cent, final_amount_cent,"
                + " status) VALUES (?, ?, ?, 'exclusive_full', ?, ?, 'draft')", "si_dwt_" + s, "sb_dwt_" + s, bar, D, D);
        EntityTaxFixture.taxDeclaredForAllMonths(jdbc, bar);
        jdbc.update("INSERT INTO dividend_plans (id, merchant_id, principal_user_id, platform_share_bps, period_type, revenue_domains,"
                + " include_inventory_cost, status, payout_pay_slot, effective_from, reason, period_min_months, period_max_months, principal_type)"
                + " VALUES (?, ?, ?, 6000, 'monthly', 'ticket', TRUE, 'active', 1, DATE '2000-01-01', '工商登记持股比例', 1, 12, 'individual')",
                "divp_dwt_" + s, bar, "usr_dwt_principal_" + s);
        jdbc.update("INSERT INTO dividend_plan_shareholders (id, plan_id, shareholder_name, shareholder_type, share_bps, sort_order)"
                + " VALUES (?, ?, '股东乙', 'individual', 1000, 0), (?, ?, '丙公司', 'company', 500, 1)",
                "dps_dwt_b_" + s, "divp_dwt_" + s, "dps_dwt_c_" + s, "divp_dwt_" + s);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        finance = staffs.staff("usr_dwt_fin_" + s, "platform_admin", project, null, "finance.dividend.manage", "finance.dividend.view");
        financeReviewer = staffs.staff(financeApprover, "staff", project, null, "governance.supervisor_review.approve");
        taxReviewer = staffs.staff(taxApprover, "staff", project, null, "governance.supervisor_review.approve");
        legalReviewer = staffs.staff(legalApprover, "staff", project, null, "governance.supervisor_review.approve");
        routeLines();
    }

    /**
     * ① 税率版本列表含出厂版本 20%、上下限未配、已生效；② 提交新版本缺上下限、税率不在上下限之内各 400，法务线没有部门起点 409；三线齐全提交落审批中，
     * 同一生效起始日再提交 409；最高权限本人提交同样进审批；财务、税务、法务三条线与最高确认逐级批准后已生效。修复前没有税率版本接口：列表 404，断言红。
     */
    @Test
    void taxRateVersionsNeedFinanceTaxLegalReviewAndTopConfirmation() throws Exception {
        List<Map<String, Object>> rows = JsonPath.read(mockMvc.perform(get(RATES).with(finance))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data");
        assertThat(rows).filteredOn(row -> "dtrv_factory".equals(row.get("id"))).singleElement().satisfies(row -> {
            assertThat(row.get("rate_bps")).isEqualTo(2000);
            assertThat(row.get("min_bps")).isNull();
            assertThat(row.get("max_bps")).isNull();
            assertThat(row.get("effective_from")).isNull();
            assertThat(row.get("status")).isEqualTo("approved");
        });

        LocalDate from = businessDays.merchantBusinessDate(null, bar, Instant.now()).plusDays(1);
        submit(finance, "1a", "\"rate_bps\":2500,\"max_bps\":3000", from)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        submit(finance, "1b", "\"rate_bps\":3500,\"min_bps\":1000,\"max_bps\":3000", from)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        jdbc.update("UPDATE approval_lines SET entry_user_id = NULL WHERE line_code = 'legal_review'");
        submit(finance, "1c", "\"rate_bps\":2500,\"min_bps\":1000,\"max_bps\":3000", from)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("TAX_DEPARTMENT_ROUTING_REQUIRED"));
        routeLines();

        String body = submit(finance, "1d", "\"rate_bps\":2500,\"min_bps\":1000,\"max_bps\":3000", from)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"))
                .andExpect(jsonPath("$.data.rate_bps").value(2500))
                .andExpect(jsonPath("$.data.min_bps").value(1000))
                .andExpect(jsonPath("$.data.max_bps").value(3000))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(body, "$.data.id");
        String review = JsonPath.read(body, "$.data.review_id");
        assertThat(review).isNotBlank();
        submit(finance, "1e", "\"rate_bps\":2000,\"min_bps\":1000,\"max_bps\":3000", from)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("DIVIDEND_TAX_RATE_VERSION_CONFLICT"));
        submit(master(), "1f", "\"rate_bps\":2200,\"min_bps\":1000,\"max_bps\":3000", from.plusDays(1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending_review"));

        approved(review);
        assertThat(jdbc.queryForObject("SELECT status FROM dividend_tax_rate_versions WHERE id = ?", String.class, id)).isEqualTo("approved");
    }

    /**
     * ③ 方案上不再有税率：先按出厂 20% 出 8 月正式单，单上固化税率版本 dtrv_factory；再经三审与最高确认生效一版 25%（1000～3000，自 2000-01-02 起）后重算，
     * 新单按出单当日已生效的 25% 代扣：个人主理人与个人股东乙按 25% 整除截断，公司股东丙代扣 0，单上固化新版本号；作废的旧单仍记 20% 与出厂版本。
     * 修复前出单取方案税率、单上没有税率版本：方案列表仍有 tax_rate_bps，断言红。
     */
    @Test
    void officialStatementWithholdsAtUnifiedRateInForceOnIssueDayAndRecordsVersion() throws Exception {
        mockMvc.perform(get("/api/v1/finance/dividends/plans?merchant_id=" + bar).with(finance))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tax_rate_bps").doesNotExist());
        String first = JsonPath.read(compute("3a")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed[0].tax_rate_bps").value(2000))
                .andExpect(jsonPath("$.data.computed[0].tax_rate_version_id").value("dtrv_factory"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.computed[0].id");

        String body = submit(finance, "3b", "\"rate_bps\":2500,\"min_bps\":1000,\"max_bps\":3000", LocalDate.of(2000, 1, 2))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String version = JsonPath.read(body, "$.data.id");
        approved(JsonPath.read(body, "$.data.review_id"));

        String statement = JsonPath.read(compute("3c")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed[0].tax_rate_bps").value(2500))
                .andExpect(jsonPath("$.data.computed[0].tax_rate_version_id").value(version))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.computed[0].id");
        assertThat(jdbc.queryForObject("SELECT tax_cent FROM dividend_statements WHERE id = ?", Long.class, statement))
                .isEqualTo(PRINCIPAL * 2500 / 10000);
        assertThat(jdbc.queryForList("SELECT amount_cent FROM dividend_statement_lines WHERE statement_id = ? AND line_type = 'shareholder_tax_withheld'"
                + " ORDER BY seq", Long.class, statement)).containsExactly(-(B_GROSS * 2500 / 10000), 0L);
        assertThat(jdbc.queryForMap("SELECT status, tax_rate_bps, tax_rate_version_id FROM dividend_statements WHERE id = ?", first))
                .containsEntry("status", "voided").containsEntry("tax_rate_bps", 2000).containsEntry("tax_rate_version_id", "dtrv_factory");
    }

    // ---------- helpers ----------

    /** 财务、税务、法务三条线的真实部门起点（在用、具正式审批角色）。 */
    private void routeLines() {
        for (String[] line : List.of(new String[]{"finance_review", financeApprover}, new String[]{"tax_review", taxApprover},
                new String[]{"legal_review", legalApprover})) {
            jdbc.update("INSERT INTO approval_lines(line_code,line_name,min_start_rank,entry_user_id,status) VALUES(?,?,1,?,'active')"
                    + " ON CONFLICT(line_code) DO UPDATE SET entry_user_id=EXCLUDED.entry_user_id,status='active'", line[0], line[0], line[1]);
        }
    }

    /** 持股分红页「分红代扣个税税率」面板「提交新税率」的真实请求形状（蛇形键），带财务、税务、法务三条线最终审批人。 */
    private ResultActions submit(RequestPostProcessor who, String tag, String rate, LocalDate from) throws Exception {
        return mockMvc.perform(post(RATES).with(who).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"dwt-" + tag + "-" + s + "\"," + rate + ",\"effective_from\":\"" + from
                        + "\",\"reason\":\"依据个人所得税法调整股息红利代扣税率\",\"parallel_lines\":["
                        + "{\"line_code\":\"finance_review\",\"final_approver_id\":\"" + financeApprover + "\"},"
                        + "{\"line_code\":\"tax_review\",\"final_approver_id\":\"" + taxApprover + "\"},"
                        + "{\"line_code\":\"legal_review\",\"final_approver_id\":\"" + legalApprover + "\"}]}"));
    }

    /** 财务、税务、法务三条线的审批人与最高确认各用自己的会话，经真实审批接口逐级批准。 */
    private void approved(String review) throws Exception {
        for (RequestPostProcessor reviewer : List.of(financeReviewer, taxReviewer, legalReviewer, master())) {
            mockMvc.perform(post("/api/v1/platform/supervisor-review/" + review + "/approve").with(reviewer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"decision\":\"approved\",\"comment\":\"已核对税法依据\",\"idempotency_key\":\"dwt-ap-" + UUID.randomUUID() + "\"}"))
                    .andExpect(status().isOk());
        }
    }

    /** 分红页期间作业「正式单」的真实请求形状（蛇形键）。 */
    private ResultActions compute(String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/dividends/statements/compute").with(finance).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"dwt-c-" + tag + "-" + s + "\",\"period_label\":\"2026-08\",\"kind\":\"official\","
                        + "\"merchant_id\":\"" + bar + "\"}"));
    }

    private RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
