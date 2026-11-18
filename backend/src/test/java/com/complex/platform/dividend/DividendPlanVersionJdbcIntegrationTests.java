package com.complex.platform.dividend;

// 文件版本：V0.01

import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.BusinessDayResolver;
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
 * 第十八批 AQ 段第 234 条 234-R1 分红方案版本化真库回归。依据：BSM:521「每次改动生成新版本，必须填写生效起始日期，并先存为草案、经高敏权限“转正”审批
 * 通过后按指定日期生效，不即改即生效。新版本只对生效日之后发生的业务适用，已发生的业务一律按当时生效的版本计算；每一笔单据在生成时就把所用配置版本固化在单上」；
 * BSM:1775「必须以获批登记版本生效，修改不能追溯已经确认或发放的分红」；PGM:666「持股分红的方案配置……由最高权限审批，提交人不能审批自己的单据」；
 * PGM:529「涉钱审批出厂默认 1 个工作日」；BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」。
 *
 * <p>夹具随测试事务回滚：综合体与酒吧建档 2026-05-01 前，酒吧有自 2000-01-01 起已批准的月度方案 V1（平台八成）。财务（持 finance.dividend.manage）
 * 有直接上级；请求按分红页方案弹窗的蛇形键走真实入口与授权链，审批走真实复核接口。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class DividendPlanVersionJdbcIntegrationTests {

    private static final String PLANS = "/api/v1/finance/dividends/plans";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BusinessDayResolver businessDays;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "dpv_prj_" + s;
    private final String bar = "dpv_bar_" + s;
    private final String v1 = "divp_dpv_v1_" + s;
    private final String financeId = "usr_dpv_fin_" + s;
    private RequestPostProcessor finance;
    private RequestPostProcessor superior;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at)"
                + " VALUES(?,'complex','分红版本综合体','active',TIMESTAMPTZ '2026-04-01 00:00:00+08',now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at)"
                + " VALUES(?,?,'bar','active',?,?,now())", bar, "分红版本酒吧" + s, project, Timestamp.from(Instant.parse("2026-04-30T16:00:00Z")));
        version(v1, "2000-01-01", "active", 8000);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        finance = staffs.staff(financeId, "platform_admin", project, null, "finance.dividend.manage", "finance.dividend.view",
                "governance.supervisor_review.approve", "governance.supervisor_review.view");
        String superiorId = "usr_dpv_sup_" + s;
        superior = staffs.staff(superiorId, "staff", project, null, "governance.supervisor_review.approve", "governance.supervisor_review.view");
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", financeId, superiorId);
    }

    /** ① 财务在已批准版本上提交调整：落一个待最高权限审批的新版本，原版本不动。修复前原地改 V1、返回 active：status 断言红。 */
    @Test
    void financeSubmitsNewVersionIntoTopAuthorityReviewLeavingCurrentVersionInForce() throws Exception {
        String id = idOf(submit(finance, v1, body("dpv-1-" + s, 7000, tomorrow(), "主理人增资，平台比例调为七成"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review")));
        assertThat(id).isNotEqualTo(v1);
        Map<String, Object> row = jdbc.queryForMap("SELECT status, platform_share_bps, review_id, reason FROM dividend_plans WHERE id = ?", id);
        assertThat(row).containsEntry("status", "pending_review").containsEntry("platform_share_bps", 7000)
                .containsEntry("reason", "主理人增资，平台比例调为七成");
        assertThat(row.get("review_id")).isNotNull();
        assertThat(jdbc.queryForMap("SELECT status, platform_share_bps FROM dividend_plans WHERE id = ?", v1))
                .containsEntry("status", "active").containsEntry("platform_share_bps", 8000);
        assertThat(jdbc.queryForMap("SELECT action_type, approval_level, object_type, object_id FROM supervisor_review_decisions WHERE review_id = ?",
                row.get("review_id")))
                .containsEntry("action_type", "finance.dividend.plan.change").containsEntry("approval_level", "top_authority")
                .containsEntry("object_type", "dividend_plan").containsEntry("object_id", id);
    }

    /** ② 生效起始日须晚于当前营业日、理由必填；已有待审批版本、在待审版本上再提交、给已有方案的商户新建，各返回对应业务码。 */
    @Test
    void versionSubmissionValidatesDateReasonAndVersionState() throws Exception {
        submit(finance, v1, body("dpv-2a-" + s, 7000, today(), "生效日填当天"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        submit(finance, v1, "{\"idempotency_key\":\"dpv-2b-" + s + "\",\"platform_share_bps\":7000,\"payout_pay_slot\":1,"
                + "\"effective_from\":\"" + tomorrow() + "\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(versions()).isEqualTo(1);

        String pending = idOf(submit(finance, v1, body("dpv-2c-" + s, 7000, tomorrow(), "比例调整")).andExpect(status().isOk()));
        submit(finance, v1, body("dpv-2d-" + s, 6000, tomorrow().plusDays(1), "再次调整"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("DIVIDEND_PLAN_PENDING_EXISTS"));
        submit(finance, pending, body("dpv-2e-" + s, 6000, tomorrow().plusDays(1), "在待审版本上调整"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("DIVIDEND_PLAN_NOT_EDITABLE"));
        mockMvc.perform(post(PLANS).with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dpv-2f-" + s + "\",\"merchant_id\":\"" + bar + "\",\"principal_user_id\":\"usr_dpv_principal\","
                                + "\"period_type\":\"monthly\",\"period_min_months\":1,\"period_max_months\":12,\"payout_pay_slot\":1,"
                                + "\"effective_from\":\"" + tomorrow() + "\",\"reason\":\"重复新建\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("DIVIDEND_PLAN_EXISTS"));
        assertThat(versions()).isEqualTo(2);
    }

    /** ③ 提交人不能审批自己的单；上级与最高权限依次批准后新版本已批准、待生效；另一版本被驳回记已驳回。 */
    @Test
    void topAuthorityApprovalAppliesVersionAndSubmitterCannotApprove() throws Exception {
        String id = idOf(submit(finance, v1, body("dpv-3-" + s, 7000, tomorrow(), "比例调整")).andExpect(status().isOk()));
        String review = reviewOf(id);
        decide(finance, review, "approved")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        decide(superior, review, "approved").andExpect(status().isOk());
        assertThat(statusOf(id)).isEqualTo("pending_review");
        decide(master(), review, "approved").andExpect(status().isOk());
        assertThat(jdbc.queryForMap("SELECT status, decided_by FROM dividend_plans WHERE id = ?", id))
                .containsEntry("status", "active").containsEntry("decided_by", SessionIssuer.MASTER_USER_ID);
        mockMvc.perform(get(PLANS + "?merchant_id=" + bar).with(finance))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + v1 + "')].version_state").value("in_force"))
                .andExpect(jsonPath("$.data[?(@.id == '" + id + "')].version_state").value("scheduled"));

        String rejected = idOf(submit(finance, v1, body("dpv-3r-" + s, 6000, tomorrow().plusDays(5), "再调六成"))
                .andExpect(status().isOk()));
        decide(superior, reviewOf(rejected), "rejected").andExpect(status().isOk());
        assertThat(statusOf(rejected)).isEqualTo("rejected");
    }

    /** ④ 审批通过时生效起始日已到：记审批时已过生效日（expired），不生效，V1 仍在生效。 */
    @Test
    void approvalAfterEffectiveDateExpiresVersion() throws Exception {
        String id = idOf(submit(finance, v1, body("dpv-4-" + s, 7000, tomorrow(), "比例调整")).andExpect(status().isOk()));
        jdbc.update("UPDATE dividend_plans SET effective_from = ? WHERE id = ?", today(), id);
        String review = reviewOf(id);
        decide(superior, review, "approved").andExpect(status().isOk());
        decide(master(), review, "approved").andExpect(status().isOk());
        assertThat(statusOf(id)).isEqualTo("expired");
        mockMvc.perform(get(PLANS + "?merchant_id=" + bar).with(finance))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + v1 + "')].version_state").value("in_force"))
                .andExpect(jsonPath("$.data[?(@.id == '" + id + "')].version_state").value("expired"));
    }

    /** ⑤ 最高权限本人提交直接生效、不产生复核单；同商户同一生效起始日再提交 409。 */
    @Test
    void masterSubmissionTakesEffectWithoutReviewAndSameDateConflicts() throws Exception {
        String id = idOf(submit(master(), v1, body("dpv-5-" + s, 7000, tomorrow(), "最高权限本人调整"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("active")));
        assertThat(jdbc.queryForMap("SELECT review_id, decided_by FROM dividend_plans WHERE id = ?", id))
                .containsEntry("review_id", null).containsEntry("decided_by", SessionIssuer.MASTER_USER_ID);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM supervisor_review_decisions WHERE object_type = 'dividend_plan' AND object_id = ?",
                Integer.class, id)).isZero();
        submit(master(), v1, body("dpv-5b-" + s, 6000, tomorrow(), "同日再调"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("DIVIDEND_PLAN_VERSION_CONFLICT"));
    }

    /**
     * ⑥ 每期出单取该期期末日已生效的版本：V1 自 2000-01-01（八成）、V2 自 2026-09-01（七成）；8 月单用 V1、9 月单用 V2；自 2026-10-01 起停用后
     * 10 月出单 404，9 月草稿重算仍用 V2。修复前同商户多个生效行只取其一：两期 plan_id 至少一个断言红。
     */
    @Test
    void statementUsesVersionInForceAtPeriodEnd() throws Exception {
        String v2 = "divp_dpv_v2_" + s;
        version(v2, "2026-09-01", "active", 7000);
        markRangeFinal(jdbc, "bar", bar, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 30));
        EntityTaxFixture.taxDeclaredForAllMonths(jdbc, bar);

        compute("2026-08", "dpv-6a-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed[0].plan_id").value(v1))
                .andExpect(jsonPath("$.data.computed[0].platform_share_bps").value(8000));
        compute("2026-09", "dpv-6b-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed[0].plan_id").value(v2))
                .andExpect(jsonPath("$.data.computed[0].platform_share_bps").value(7000));

        version("divp_dpv_v3_" + s, "2026-10-01", "ended", 7000);
        compute("2026-10", "dpv-6c-" + s)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail_code").value("DIVIDEND_PLAN_NOT_FOUND"));
        compute("2026-09", "dpv-6d-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed[0].plan_id").value(v2));
    }

    /** ⑧ 分红方案调整进审批时限清单，按涉钱出厂 1 个工作日（PGM:529）。修复前清单无此类型：断言红。 */
    @Test
    void planChangeIsMoneyApprovalTypeInDeadlineList() throws Exception {
        List<Map<String, Object>> rows = JsonPath.read(mockMvc.perform(get("/api/v1/platform/approval-deadlines").with(finance))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.data[?(@.action_type == 'finance.dividend.plan.change')]");
        assertThat(rows).singleElement().satisfies(row ->
                assertThat(row).containsEntry("money", true).containsEntry("factory_working_days", 1));
    }

    // ---------- helpers ----------

    /** 已批准版本夹具（月度、上下限月～年、只计酒吧收入、扣销货成本与全部费用、第一个付账日）。 */
    private void version(String id, String effectiveFrom, String status, int platformShareBps) {
        jdbc.update("INSERT INTO dividend_plans (id, merchant_id, principal_user_id, platform_share_bps, period_type, revenue_domains,"
                + " include_inventory_cost, status, payout_pay_slot, effective_from, reason, period_min_months, period_max_months)"
                + " VALUES (?, ?, 'usr_dpv_principal', ?, 'monthly', 'bar', TRUE, ?, 1, ?::date, '工商登记持股比例', 1, 12)",
                id, bar, platformShareBps, status, effectiveFrom);
    }

    private LocalDate today() {
        return businessDays.merchantBusinessDate(null, bar, Instant.now());
    }

    private LocalDate tomorrow() {
        return today().plusDays(1);
    }

    /** 分红页方案弹窗「提交最高权限审批」的真实请求形状（蛇形键）。 */
    private static String body(String key, int platformShareBps, LocalDate effectiveFrom, String reason) {
        return "{\"idempotency_key\":\"" + key + "\",\"platform_share_bps\":" + platformShareBps + ",\"payout_pay_slot\":1,"
                + "\"status\":\"active\",\"effective_from\":\"" + effectiveFrom + "\",\"reason\":\"" + reason + "\"}";
    }

    private ResultActions submit(RequestPostProcessor who, String planId, String body) throws Exception {
        return mockMvc.perform(put(PLANS + "/" + planId).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private String reviewOf(String planId) {
        return jdbc.queryForObject("SELECT review_id FROM dividend_plans WHERE id = ?", String.class, planId);
    }

    private String statusOf(String planId) {
        return jdbc.queryForObject("SELECT status FROM dividend_plans WHERE id = ?", String.class, planId);
    }

    private int versions() {
        return jdbc.queryForObject("SELECT count(*) FROM dividend_plans WHERE merchant_id = ?", Integer.class, bar);
    }

    /** 主管复核页「通过／驳回」的真实请求形状（蛇形键）。 */
    private ResultActions decide(RequestPostProcessor who, String review, String decision) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + review + "/approve").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"" + decision + "\",\"comment\":\"已核对工商登记\",\"idempotency_key\":\"dpv-ap-" + UUID.randomUUID() + "\"}"));
    }

    /** 分红计算页期间作业「正式单」的真实请求形状（蛇形键）。 */
    private ResultActions compute(String label, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/dividends/statements/compute").with(finance)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\",\"period_label\":\"" + label + "\",\"kind\":\"official\","
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
