package com.complex.platform.finance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.LocalZone;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
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
 * 第十八批 Y 段第 181 条 181-R2（ENUM:191「待识别与待处理两个阶段各自的停留时限为业务参数，按项目可配，出厂默认 1 个工作日，须同时配置上下限；
 * 超时未推进的，系统自动提醒并升级到上一级负责人。」）：对账差异待识别、待处理两阶段停留时限与超时升级。配置时限、识别、提交复核、审批都按后台真实请求
 * （蛇形键）走真实入口与授权链；超时扫描调定时作业的业务入口 {@link FinanceReconciliationDifferenceService#escalateOverdue}。
 * 时点取月中工作日：本阶段起始 2026-03-11（周三）10:00，扫描时刻 2026-03-13（周五）10:00＝起始后 2 个工作日。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FinanceReconciliationEscalationJdbcTests {

    private static final ZonedDateTime ENTERED = ZonedDateTime.of(2026, 3, 11, 10, 0, 0, 0, LocalZone.ID);
    private static final Instant TWO_WORKING_DAYS_LATER = ENTERED.plusDays(2).toInstant();

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private FinanceReconciliationDifferenceService differences;

    private final String s = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "fre_pa_" + s;
    private final String otherProject = "fre_pb_" + s;
    private final String bar = "fre_ma_" + s;
    private final String financeId = "usr_fre_fin_" + s;
    private final String superiorId = "usr_fre_sup_" + s;
    private final String batch = "fre_b_" + s;
    private RequestPostProcessor admin;
    private RequestPostProcessor finance;
    private RequestPostProcessor superior;
    private RequestPostProcessor otherAdmin;
    private RequestPostProcessor viewer;
    private StaffRequestFixture staffs;

    @BeforeEach
    void seed() {
        for (String p : List.of(project, otherProject)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','对账差异时限项目','active',now(),now())", p);
        }
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at,project_id)"
                + " VALUES(?,'对账差异时限酒吧','bar','active',now(),now(),?)", bar, project);
        staffs = new StaffRequestFixture(jdbc);
        admin = staffs.staff("usr_fre_admin_" + s, "platform_admin", project, null, "finance.reconciliation.policy.manage");
        otherAdmin = staffs.staff("usr_fre_oth_" + s, "staff", otherProject, null, "finance.reconciliation.policy.manage");
        // 负责人（识别人、提交复核人）与其直接上级（复核审批人）。负责人按平台管理员建；负责人与上级都是平台侧普通员工的见 ⑧。
        finance = staffs.staff(financeId, "platform_admin", project, null, "finance.reconciliation.view", "finance.reconciliation.resolve");
        viewer = staffs.staff("usr_fre_view_" + s, "staff", project, null, "finance.reconciliation.view");
        superior = staffs.staff(superiorId, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        profile(financeId);
        profile(superiorId);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", financeId, superiorId);
        jdbc.update("INSERT INTO finance_reconciliation_batches(id,batch_no,scope_type,scope_id,business_date,status,created_at)"
                + " VALUES(?,?,'bar',?,?,'created',now())", batch, "FRE-" + s, bar, LocalDate.of(2026, 3, 10));
    }

    /** ① 无配置按出厂 1 个工作日：超时后负责人改为直接上级，原负责人与上级各收一条员工系统通知，列表带本阶段超时升级时刻；再跑一轮不重复。 */
    @Test
    void overdueWithoutConfigEscalatesToSuperiorOnce() throws Exception {
        String diff = difference("pending_identification", "manual", financeId);

        differences.escalateOverdue(TWO_WORKING_DAYS_LATER);

        Map<String, Object> row = row(diff);
        assertThat(row.get("responsible_user_id")).isEqualTo(superiorId);
        assertThat(row.get("identify_escalated_at")).isNotNull();
        assertThat(notices(financeId, diff)).isEqualTo(1);
        assertThat(notices(superiorId, diff)).isEqualTo(1);
        String body = mockMvc.perform(get("/api/v1/finance/reconciliation/differences").with(viewer))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Object> escalatedAt = JsonPath.read(body, "$.data.items[?(@.id == '" + diff + "')].stage_escalated_at");
        assertThat(escalatedAt).hasSize(1).doesNotContainNull();

        differences.escalateOverdue(ENTERED.plusDays(9).toInstant());

        assertThat(row(diff).get("responsible_user_id")).isEqualTo(superiorId);
        assertThat(notices(financeId, diff)).isEqualTo(1);
        assertThat(notices(superiorId, diff)).isEqualTo(1);
    }

    /** ② 给项目配待识别 3 个工作日（后台真实请求）后同一时点不升级；读回两阶段，未配置的待处理按出厂 1 个工作日、上下限为空；别的项目账号改不了。 */
    @Test
    void projectConfiguredLimitDefersEscalation() throws Exception {
        putLimit(admin, project, "pending_identification", 3, 1, 5, "cfg").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project_id").value(project))
                .andExpect(jsonPath("$.data.working_days").value(3));
        mockMvc.perform(get("/api/v1/finance/reconciliation/stage-limits").param("project_id", project).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stage").value("pending_identification"))
                .andExpect(jsonPath("$.data[0].working_days").value(3))
                .andExpect(jsonPath("$.data[0].max_working_days").value(5))
                .andExpect(jsonPath("$.data[1].stage").value("pending_handling"))
                .andExpect(jsonPath("$.data[1].working_days").value(1))
                .andExpect(jsonPath("$.data[1].min_working_days").doesNotExist());
        putLimit(otherAdmin, project, "pending_identification", 2, 1, 5, "other").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        String diff = difference("pending_identification", "manual", financeId);

        differences.escalateOverdue(TWO_WORKING_DAYS_LATER);

        Map<String, Object> row = row(diff);
        assertThat(row.get("responsible_user_id")).isEqualTo(financeId);
        assertThat(row.get("identify_escalated_at")).isNull();
        assertThat(notices(financeId, diff)).isZero();
    }

    /** ③ 时限 0、时限不在上下限内、只填时限不填上下限 → 400。 */
    @Test
    void invalidLimitsAreRejected() throws Exception {
        putLimit(admin, project, "pending_handling", 0, 1, 5, "zero").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        putLimit(admin, project, "pending_handling", 6, 1, 5, "out").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        putLimit(admin, project, "pending_handling", 2, null, null, "half").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM finance_reconciliation_stage_limits WHERE project_id = ?", Integer.class, project))
                .isZero();
    }

    /** ④ 负责人没有直接上级：只提醒本人，负责人不变，本阶段记已升级（不再重复提醒）。 */
    @Test
    void responsibleWithoutSuperiorIsOnlyReminded() throws Exception {
        String lone = "usr_fre_lone_" + s;
        staffs.staff(lone, "staff", project, null, "finance.reconciliation.resolve");
        profile(lone);
        String diff = difference("pending_identification", "manual", lone);

        differences.escalateOverdue(TWO_WORKING_DAYS_LATER);
        differences.escalateOverdue(ENTERED.plusDays(9).toInstant());

        Map<String, Object> row = row(diff);
        assertThat(row.get("responsible_user_id")).isEqualTo(lone);
        assertThat(row.get("identify_escalated_at")).isNotNull();
        assertThat(notices(lone, diff)).isEqualTo(1);
    }

    /** ⑤ 识别后按待处理时限另算：待处理配 3 个工作日，起始后 2 个工作日不升级，过了 3 个工作日才升级待处理一列。 */
    @Test
    void handlingStageUsesItsOwnLimit() throws Exception {
        putLimit(admin, project, "pending_handling", 3, 1, 5, "handle").andExpect(status().isOk());
        String diff = difference("pending_identification", "manual", financeId);
        identify(diff, "h").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending_handling"));
        jdbc.update("UPDATE finance_reconciliation_differences SET stage_entered_at = ? WHERE id = ?", Timestamp.from(ENTERED.toInstant()), diff);

        differences.escalateOverdue(TWO_WORKING_DAYS_LATER);
        assertThat(row(diff).get("handle_escalated_at")).isNull();

        differences.escalateOverdue(ENTERED.plusDays(7).toInstant());
        Map<String, Object> row = row(diff);
        assertThat(row.get("handle_escalated_at")).isNotNull();
        assertThat(row.get("identify_escalated_at")).isNull();
        assertThat(row.get("responsible_user_id")).isEqualTo(superiorId);
    }

    /** ⑥ 待复核（统一审批管时限）与已关闭不扫。 */
    @Test
    void reviewingAndClosedDifferencesAreNotScanned() throws Exception {
        String reviewing = difference("pending_review", "manual", financeId);
        String closed = difference("closed", "manual", financeId);

        differences.escalateOverdue(ENTERED.plusDays(30).toInstant());

        for (String diff : List.of(reviewing, closed)) {
            Map<String, Object> row = row(diff);
            assertThat(row.get("responsible_user_id")).isEqualTo(financeId);
            assertThat(row.get("identify_escalated_at")).isNull();
            assertThat(row.get("handle_escalated_at")).isNull();
        }
    }

    /** ⑦ 待处理超时升级后提交复核被驳回：退回待处理时清空待处理一列，之后再超时可再升级。 */
    @Test
    void rejectedReviewClearsHandlingEscalation() throws Exception {
        String diff = difference("pending_handling", "system", financeId);
        differences.escalateOverdue(TWO_WORKING_DAYS_LATER);
        assertThat(row(diff).get("handle_escalated_at")).isNotNull();

        String reviewId = JsonPath.read(submit(diff, "r").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"))
                .andReturn().getResponse().getContentAsString(), "$.data.review_id");
        reject(reviewId, "r").andExpect(status().isOk());
        Map<String, Object> returned = row(diff);
        assertThat(returned.get("status")).isEqualTo("pending_handling");
        assertThat(returned.get("handle_escalated_at")).isNull();

        differences.escalateOverdue(Instant.now().plus(Duration.ofDays(10)));
        assertThat(row(diff).get("handle_escalated_at")).isNotNull();
    }

    /**
     * ⑧ 负责人与其直接上级都是平台侧普通员工（不属任何商户）：超时升级给上级后，上级按对账差异页真实请求继续识别、提交复核（PGM:664「跨主体与平台层面的
     * 财务工作——三方对账……由财务中心负责」「单店层面的财务工作——……差异认定……由该酒吧对应的财务 BP 负责」）。
     */
    @Test
    void escalatedStaffSuperiorContinuesHandling() throws Exception {
        String clerkId = "usr_fre_clerk_" + s;
        String leadId = "usr_fre_lead_" + s;
        staffs.staff(clerkId, "staff", project, null, "finance.reconciliation.view", "finance.reconciliation.resolve");
        RequestPostProcessor lead = staffs.staff(leadId, "staff", project, null, "finance.reconciliation.view", "finance.reconciliation.resolve");
        profile(clerkId);
        profile(leadId);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", clerkId, leadId);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", leadId, superiorId);
        String diff = difference("pending_identification", "system", clerkId);

        differences.escalateOverdue(TWO_WORKING_DAYS_LATER);
        assertThat(row(diff).get("responsible_user_id")).isEqualTo(leadId);

        identify(lead, diff, "lead").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_handling"))
                .andExpect(jsonPath("$.data.responsible_user_id").value(leadId));
        submit(lead, diff, "lead").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"));
    }

    // ---------- helpers ----------

    /** 前置态：该酒吧批次下一条差异，负责人与本阶段起始时刻为给定值（起始 2026-03-11 10:00）。 */
    private String difference(String status, String source, String responsible) {
        String id = "fre_d_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("""
                INSERT INTO finance_reconciliation_differences(id,batch_id,difference_type,business_ref,amount_cent,expected_amount_cent,
                    actual_amount_cent,source,status,created_at,responsible_user_id,stage_entered_at)
                VALUES(?,?,'amount',?,700,1000,300,?,?,now(),?,?)
                """, id, batch, "ref_" + id, source, status, responsible, Timestamp.from(ENTERED.toInstant()));
        return id;
    }

    private void profile(String userId) {
        jdbc.update("INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, registration_status, status, created_at, updated_at)"
                + " VALUES (?, ?, NULL, ?, '对账差异财务', 'approved', 'active', now(), now())", "stf_" + userId, project, userId);
    }

    /** 对账差异页「差异停留时限（按项目）」保存的真实请求（三值可缺）。 */
    private ResultActions putLimit(RequestPostProcessor who, String projectId, String stage, Integer days, Integer min, Integer max, String tag)
            throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("idempotency_key", "fre-limit-" + tag + "-" + s, "stage", stage, "working_days", days));
        if (min != null) {
            body.put("min_working_days", min);
        }
        if (max != null) {
            body.put("max_working_days", max);
        }
        return mockMvc.perform(put("/api/v1/finance/reconciliation/stage-limits/" + projectId).with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions identify(String differenceId, String tag) throws Exception {
        return identify(finance, differenceId, tag);
    }

    private ResultActions identify(RequestPostProcessor who, String differenceId, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/reconciliation/differences/" + differenceId + "/identify").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "fre-identify-" + tag + "-" + s, "cause_note", "渠道少记一笔核销"))));
    }

    private ResultActions submit(String differenceId, String tag) throws Exception {
        return submit(finance, differenceId, tag);
    }

    private ResultActions submit(RequestPostProcessor who, String differenceId, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/reconciliation/differences/" + differenceId + "/close").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "fre-submit-" + tag + "-" + s, "close_reason", "以平台口径为准",
                        "evidence_attachment_ids", List.of()))));
    }

    private ResultActions reject(String reviewId, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + reviewId + "/approve").with(superior)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "fre-reject-" + tag + "-" + s, "decision", "rejected",
                        "comment", "处理结论与原件不符"))));
    }

    private Map<String, Object> row(String differenceId) {
        return jdbc.queryForMap("SELECT status, responsible_user_id, identify_escalated_at, handle_escalated_at"
                + " FROM finance_reconciliation_differences WHERE id = ?", differenceId);
    }

    private int notices(String userId, String differenceId) {
        return jdbc.queryForObject("SELECT count(*) FROM employee_notices WHERE staff_profile_id = ? AND title = ?", Integer.class,
                "stf_" + userId, "对账差异超时：" + differenceId);
    }
}
