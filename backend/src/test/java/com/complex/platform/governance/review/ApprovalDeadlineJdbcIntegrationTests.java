package com.complex.platform.governance.review;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AD 段第 204 条 204-R1（PGM:529「任一业务审批节点超过后台配置的时限（业务参数，按审批类型可配，出厂默认 3 个工作日，涉钱审批出厂默认
 * 1 个工作日，须同时配置上下限）未处理的，系统自动提醒并升级到该节点的上一级」；PGM:536「按合同类型可配，出厂默认 3 个工作日，须同时配置上下限」；
 * 第十八批裁决 37、39；BSM:1438「免费放行和减免须经物业主管审批」）：审批时限按被审批的动作类型可配——真库＋真实接口＋真实角色授权链，
 * 请求体按前端蛇形键。到期时刻按已审批工作日历推算（以服务的 addWorkingDays 为准，不写死日期）。
 *
 * <p>夹具随测试事务回滚（时限表、账号、角色、复核单一并回滚）；被拒请求的失败留痕是独立事务，按带随机后缀的提交编号隔离。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ApprovalDeadlineJdbcIntegrationTests {

    private static final String URL = "/api/v1/platform/approval-deadlines";

    @Autowired private AccessRepository accessRepository;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SupervisorReviewService reviews;

    private final String suffix = Long.toString(System.nanoTime());
    private String adminId;
    /** 平台管理员（role_platform_admin），发起授权申请用。 */
    private RequestPostProcessor admin;
    /** 角色持 admission.policy.manage（与授权政策写口同一权限；默认管理员角色里的高危条目不算逐项授予）。 */
    private RequestPostProcessor manager;
    /** 只持 governance.supervisor_review.view。 */
    private RequestPostProcessor viewer;

    @BeforeEach
    void seed() {
        adminId = "usr_r204_admin_" + suffix;
        accessRepository.createUser(new AccessUser(adminId, adminId, "platform_admin", "org", null, "active", Instant.now()));
        accessRepository.assignUserRole(adminId, "role_platform_admin");
        admin = session(new AccessActor(adminId, adminId, "platform_admin", "org", null, "active"));
        String viewerId = "usr_r204_viewer_" + suffix;
        String role = "role_r204_viewer_" + suffix;
        accessRepository.createRole(new RoleRecord(role, "审批时限查看回归" + suffix, null, "project", "active", Instant.now(),
                List.of("governance.supervisor_review.view"), null, false, List.of()));
        accessRepository.createUser(new AccessUser(viewerId, viewerId, "staff", "org", null, "active", Instant.now()));
        accessRepository.assignUserRole(viewerId, role);
        viewer = session(new AccessActor(viewerId, viewerId, "staff", "org", null, "active"));
        String managerId = "usr_r204_manager_" + suffix;
        String managerRole = "role_r204_manager_" + suffix;
        accessRepository.createRole(new RoleRecord(managerRole, "审批时限配置回归" + suffix, null, "project", "active", Instant.now(),
                List.of("admission.policy.manage", "governance.supervisor_review.view"), null, false, List.of()));
        accessRepository.createUser(new AccessUser(managerId, managerId, "staff", "org", null, "active", Instant.now()));
        accessRepository.assignUserRole(managerId, managerRole);
        manager = session(new AccessActor(managerId, managerId, "staff", "org", null, "active"));
    }

    // ① 清单按动作类型列全部审批类型：出厂涉钱 1、其余 3 个工作日，上下限 1～30，未改过；含 18Y、18AA、18AB 新增动作与本条新定涉钱的三项；
    // 不含授权档位模板 permission_grant_l1～l3。
    @Test
    void listsEveryApprovalTypeWithFactoryDefaults() throws Exception {
        Map<String, Map<String, Object>> rows = list(viewer);
        Map<String, Integer> factory = new LinkedHashMap<>();
        factory.put("staff.payroll.adjustment.add", 1);
        factory.put("bill_waiver", 1);
        factory.put("bar.bill_waiver", 1);
        factory.put("access.grant_request", 3);
        factory.put("access.grant_request.money", 1);
        factory.put("contract.new", 3);
        factory.put("bar.bill.unpaid_close", 1);
        factory.put("finance.reconciliation_difference.close", 1);
        factory.put("bar.table_session.transfer_over_limit", 3);
        factory.put("bar.bill_reopen", 1);
        factory.put("parking.release.waive", 1);
        factory.forEach((type, days) -> assertThat(rows.get(type)).as(type)
                .containsEntry("money", days == 1).containsEntry("factory_working_days", days)
                .containsEntry("deadline_working_days", days).containsEntry("min_working_days", 1)
                .containsEntry("max_working_days", 30).containsEntry("customized", false));
        assertThat(rows).doesNotContainKeys("permission_grant_l1", "permission_grant_l2", "permission_grant_l3");
    }

    // ② 奖惩复核改为 5 个工作日（下限 2、上限 10）后，经真实接口发起的奖惩复核到期＝发起时刻加 5 个工作日；到期升级后仍按 5 个工作日顺延。
    @Test
    void configuredDeadlineDrivesPayrollAdjustmentDueAndEscalation() throws Exception {
        String type = "staff.payroll.adjustment.add";
        change(manager, type, "pay", 5, 2, 10).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.action_type").value(type))
                .andExpect(jsonPath("$.data.factory_working_days").value(1))
                .andExpect(jsonPath("$.data.deadline_working_days").value(5))
                .andExpect(jsonPath("$.data.customized").value(true));
        assertThat(list(viewer).get(type)).containsEntry("deadline_working_days", 5).containsEntry("min_working_days", 2)
                .containsEntry("max_working_days", 10).containsEntry("customized", true);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'governance.approval_deadline.changed'"
                + " AND event_result = 'success' AND idempotency_key = ?", Integer.class, "r204-pay-" + suffix)).isEqualTo(1);

        String entity = "be_r204_" + suffix;
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, '审批时限回归公司', 'project_company', 'active', now(), now())", entity);
        String hrId = "usr_r204_hr_" + suffix;
        String hrRole = "role_r204_hr_" + suffix;
        accessRepository.createRole(new RoleRecord(hrRole, "审批时限奖惩回归" + suffix, null, "project", "active", Instant.now(),
                List.of("staff.payroll.manage"), null, false, List.of()));
        accessRepository.createUser(new AccessUser(hrId, hrId, "staff", entity, null, "active", Instant.now()));
        accessRepository.assignUserRole(hrId, hrRole);
        String employee = "usr_r204_e_" + suffix;
        accessRepository.createUser(new AccessUser(employee, employee, "staff", entity, null, "active", Instant.now()));
        jdbc.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id, registration_status, status, created_at, updated_at)
                VALUES (?, ?, NULL, ?, '时限回归员工', ?, 'approved', 'active', now(), now())""", "stf_r204_" + suffix, entity, employee, entity);
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id, updated_by) VALUES (?, ?, ?)", employee, hrId, hrId);

        Instant before = Instant.now().truncatedTo(ChronoUnit.MICROS);
        String reviewId = reviewIdOf(mockMvc.perform(post("/api/v1/staff/payroll/adjustments")
                .with(session(new AccessActor(hrId, hrId, "staff", entity, null, "active"))).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"idempotency_key": "r204-adj-%s", "user_id": "%s", "month": "2099-08", "adj_type": "bonus", "amount_cent": 100, "reason": "审批时限回归"}
                        """.formatted(suffix, employee))));
        Instant after = Instant.now();
        assertThat(dueAt(reviewId)).isBetween(reviews.addWorkingDays(before, 5), reviews.addWorkingDays(after, 5));

        // 超时扫描按到期时刻排序、一次最多 200 张：夹具单排到最前（同 M738 写法）。
        jdbc.update("UPDATE supervisor_review_decisions SET due_at = TIMESTAMPTZ '2000-01-01 00:00:00+00' WHERE review_id = ?", reviewId);
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        reviews.escalateOverdue(now);
        assertThat(dueAt(reviewId)).isEqualTo(reviews.addWorkingDays(now, 5));
    }

    // ③ 只改涉钱授权申请为 2 个工作日：经授权申请接口发起的涉钱申请到期 2 个工作日，不涉钱申请仍按出厂 3 个工作日（按动作类型，不串）。
    @Test
    void grantRequestDeadlinesFollowTheirOwnActionType() throws Exception {
        change(manager, "access.grant_request.money", "grant", 2, 1, 30).andExpect(status().isOk());
        String boss = "usr_r204_boss_" + suffix;
        accessRepository.createUser(new AccessUser(boss, boss, "staff", "org", null, "active", Instant.now()));
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)", adminId, boss, adminId);
        String moneyCode = "r204.grant_test.finance_" + suffix;
        String plainCode = "r204.grant_test.ops_" + suffix;
        jdbc.update("INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)"
                + " VALUES (?, '审批时限回归涉钱权限', 'finance', 'high', TRUE, 'active')", moneyCode);
        jdbc.update("INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)"
                + " VALUES (?, '审批时限回归一般权限', 'ops', 'high', TRUE, 'active')", plainCode);

        Instant before = Instant.now().truncatedTo(ChronoUnit.MICROS);
        String money = grant("money", moneyCode);
        String plain = grant("plain", plainCode);
        Instant after = Instant.now();
        assertThat(actionType(money)).isEqualTo("access.grant_request.money");
        assertThat(actionType(plain)).isEqualTo("access.grant_request");
        assertThat(dueAt(money)).isBetween(reviews.addWorkingDays(before, 2), reviews.addWorkingDays(after, 2));
        assertThat(dueAt(plain)).isBetween(reviews.addWorkingDays(before, 3), reviews.addWorkingDays(after, 3));
    }

    // ④ 时限小于下限、下限为 0、上限小于下限一律 400 且时限表不写；未知类型 404；不持 admission.policy.manage 403 并留失败痕迹。
    @Test
    void invalidUnknownOrUnauthorizedChangesAreRejected() throws Exception {
        String type = "staff.payroll.adjustment.add";
        int[][] invalid = {{1, 2, 10}, {3, 0, 10}, {3, 5, 2}};
        for (int i = 0; i < invalid.length; i++) {
            change(manager, type, "bad" + i, invalid[i][0], invalid[i][1], invalid[i][2]).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.message").value("审批时限须在上下限之间，下限不小于 1 个工作日"));
        }
        change(manager, "no.such.approval", "unknown", 3, 1, 30).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("APPROVAL_TYPE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("没有这种审批类型"));
        change(viewer, type, "denied", 3, 1, 30).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM approval_deadline_settings WHERE action_type IN (?, 'no.such.approval')",
                Integer.class, type)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'governance.approval_deadline.change_failed'"
                + " AND event_result = 'failure' AND idempotency_key = ?", Integer.class, "r204-denied-" + suffix)).isEqualTo(1);
    }

    /** 后台主管复核页「审批时限」区块的清单：按 action_type 取行。 */
    private Map<String, Map<String, Object>> list(RequestPostProcessor who) throws Exception {
        List<Map<String, Object>> data = JsonPath.read(mockMvc.perform(get(URL).with(who)).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data");
        Map<String, Map<String, Object>> rows = new LinkedHashMap<>();
        data.forEach(row -> rows.put((String) row.get("action_type"), row));
        return rows;
    }

    /** 「修改」保存：体 idempotency_key、deadline_working_days、min_working_days、max_working_days。 */
    private ResultActions change(RequestPostProcessor who, String type, String key, int days, int min, int max) throws Exception {
        return mockMvc.perform(put(URL + "/" + type).with(who).contentType(MediaType.APPLICATION_JSON).content("""
                {"idempotency_key": "r204-%s-%s", "deadline_working_days": %d, "min_working_days": %d, "max_working_days": %d}
                """.formatted(key, suffix, days, min, max)));
    }

    /** 授权申请页「员工单独授予」：平台管理员给一名新员工申请单个须审批权限码，返回复核单号。 */
    private String grant(String tag, String code) throws Exception {
        String target = "usr_r204_target_" + tag + "_" + suffix;
        accessRepository.createUser(new AccessUser(target, target, "staff", "org", null, "active", Instant.now()));
        return reviewIdOf(mockMvc.perform(post("/api/v1/access/grant-requests").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"idempotency_key": "r204-grant-%s-%s", "request_type": "user_permission_grant", "target_user_id": "%s",
                         "permission_codes": ["%s"], "reason": "审批时限回归"}
                        """.formatted(tag, suffix, target, code))));
    }

    private static String reviewIdOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.data.review_id");
    }

    private Instant dueAt(String reviewId) {
        return jdbc.queryForObject("SELECT due_at FROM supervisor_review_decisions WHERE review_id = ?", Timestamp.class, reviewId).toInstant();
    }

    private String actionType(String reviewId) {
        return jdbc.queryForObject("SELECT action_type FROM supervisor_review_decisions WHERE review_id = ?", String.class, reviewId);
    }

    /** 后台 Web 终端＋生产过滤器放入的 AccessActor 主体（四项与账号行一致）。 */
    private static RequestPostProcessor session(AccessActor actor) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
