package com.complex.platform.governance.review;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * 第十八批 AO 段第 246 条 246-R1（PGM:529「任一业务审批节点超过后台配置的时限（业务参数，按审批类型可配……）未处理的，系统自动提醒并升级到
 * 该节点的上一级，不得默认通过，也不得默认拒绝」；PGM:536「主线与各条平行线全部通过合同才生效」）：审批时限按节点自接手时起算，平行线各自计时；
 * 超时只升级或催办已到期的节点；单据到期取各当前节点中最早的。真库＋真实接口＋真实角色授权链，请求体按前端蛇形键。
 *
 * <p>组织层级：员工 E → S1 → S2 → S3 → S4；部门人事终审人 HR；发起人 I 不在链上。奖惩复核生成主线 S1、S2、S3 与 hr_review 会签线（S4 不进链）。
 * 到期区间以服务的 deadlineFrom 为准，不写死工作日数。夹具随测试事务回滚。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ApprovalNodeDeadlineJdbcIntegrationTests {

    private static final String TYPE = SupervisorReviewService.STAFF_ADJUSTMENT_ACTION;
    private static final String LONG_AGO = "TIMESTAMPTZ '2000-01-01 00:00:00+00'";

    @Autowired private AccessRepository accessRepository;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SupervisorReviewService reviews;

    private final String suffix = Long.toString(System.nanoTime());
    private String employee;
    private String initiator;
    private String hr;
    private List<String> superiors;

    @BeforeEach
    void seed() {
        employee = "usr_r246_e_" + suffix;
        initiator = "usr_r246_i_" + suffix;
        hr = "usr_r246_hr_" + suffix;
        superiors = List.of("usr_r246_s1_" + suffix, "usr_r246_s2_" + suffix, "usr_r246_s3_" + suffix, "usr_r246_s4_" + suffix);
        String role = "role_r246_" + suffix;
        jdbc.update("INSERT INTO roles(id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '节点时限回归', 'platform', 'active', now())",
                role, "节点时限回归" + suffix);
        jdbc.update("INSERT INTO role_permissions(role_id, permission_code) SELECT ?, permission_code FROM permissions WHERE permission_code LIKE 'staff.%'"
                + " OR permission_code IN ('governance.supervisor_review.approve', 'governance.supervisor_review.view') ON CONFLICT DO NOTHING", role);
        for (String user : List.of(employee, initiator, hr, superiors.get(0), superiors.get(1), superiors.get(2), superiors.get(3))) {
            accessRepository.createUser(new AccessUser(user, user, "staff", "project_main", null, "active", Instant.now()));
            accessRepository.assignUserRole(user, role);
        }
        String below = employee;
        for (String superior : superiors) {
            jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)", below, superior, initiator);
            below = superior;
        }
        jdbc.update("INSERT INTO approval_lines(line_code, line_name, min_start_rank, status, hr_final_approver_id) VALUES (?, '节点时限回归部门', 1, 'active', ?)",
                "dept_r246_" + suffix, hr);
        jdbc.update("UPDATE org_hierarchy SET line_code = ? WHERE user_id = ?", "dept_r246_" + suffix, employee);
        jdbc.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id,
                        registration_status, status, employment_status, created_at, updated_at)
                VALUES (?, 'project_main', NULL, ?, '节点时限员工', 'project_main', 'approved', 'active', 'employed', now(), now())
                """, "stf_r246_" + suffix, employee);
    }

    // ① 首节点 S1 用掉全部时限后才通过：下一节点 S2 自接手时起算，不被立即升级；HR 会签线仍按发起时起算；单据到期取两者中较早的。
    @Test
    void nextNodeDeadlineStartsAtHandoverWhileParallelLineKeepsItsOwn() throws Exception {
        Instant initiatedFrom = Instant.now().truncatedTo(ChronoUnit.MICROS);
        String reviewId = initiate("a");
        Instant initiatedTo = Instant.now();
        // 首节点已用掉全部时限（只改单据级到期）；超时扫描按到期时刻排序、一次最多 200 张，夹具单排到最前。
        jdbc.update("UPDATE supervisor_review_decisions SET due_at = " + LONG_AGO + " WHERE review_id = ?", reviewId);

        Instant approvedFrom = Instant.now().truncatedTo(ChronoUnit.MICROS);
        approve(reviewId, superiors.get(0), "a1").andExpect(status().isOk());
        Instant approvedTo = Instant.now();
        assertThat(reviewDue(reviewId)).as("下一节点接手后单据不再沿用首节点的到期").isAfter(Instant.now());

        reviews.escalateOverdue(Instant.now());
        assertThat(node(reviewId, "main", 2)).as("S2 刚接手未到期，不升级")
                .containsEntry("approver_id", superiors.get(1)).containsEntry("escalation_count", 0);
        assertThat(node(reviewId, "hr_review", 1)).containsEntry("approver_id", hr).containsEntry("escalation_count", 0);

        Instant s2Due = nodeDue(reviewId, "main", 2);
        Instant hrDue = nodeDue(reviewId, "hr_review", 1);
        assertThat(s2Due).isBetween(reviews.deadlineFrom(TYPE, approvedFrom), reviews.deadlineFrom(TYPE, approvedTo));
        assertThat(hrDue).as("平行线自发起时起算，S1 通过不动它")
                .isBetween(reviews.deadlineFrom(TYPE, initiatedFrom), reviews.deadlineFrom(TYPE, initiatedTo));
        assertThat(reviewDue(reviewId)).isEqualTo(s2Due.isBefore(hrDue) ? s2Due : hrDue);

        List<Map<String, Object>> chain = JsonPath.read(mockMvc.perform(get("/api/v1/platform/supervisor-review/" + reviewId + "/chain")
                        .with(session(initiator))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.data");
        assertThat(chain).hasSize(4).allSatisfy(n -> assertThat(n).containsKey("due_at"));
        assertThat(chain).filteredOn(n -> "main".equals(n.get("line_code")) && Integer.valueOf(3).equals(n.get("seq")))
                .singleElement().satisfies(n -> assertThat(n.get("due_at")).as("未轮到的 S3 没有到期").isNull());
    }

    // ② 只有 HR 会签线到期：该线只有一个节点（本线最终节点），只催办并自此重新计时；主线当前节点 S1 不动；单据到期取各当前节点中最早的。
    @Test
    void onlyTheOverdueNodeIsRemindedAndMainLineNodeIsUntouched() throws Exception {
        String reviewId = initiate("b");
        Map<String, Object> s1Before = node(reviewId, "main", 1);
        Instant s1Due = nodeDue(reviewId, "main", 1);
        jdbc.update("UPDATE supervisor_review_nodes SET due_at = " + LONG_AGO + " WHERE review_id = ? AND line_code = 'hr_review'", reviewId);
        jdbc.update("UPDATE supervisor_review_decisions SET due_at = " + LONG_AGO + " WHERE review_id = ?", reviewId);

        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        reviews.escalateOverdue(now);

        Map<String, Object> hrNode = node(reviewId, "hr_review", 1);
        assertThat(hrNode).containsEntry("approver_id", hr).containsEntry("escalation_count", 0);
        Instant hrDue = nodeDue(reviewId, "hr_review", 1);
        assertThat(hrDue).isEqualTo(reviews.deadlineFrom(TYPE, now));
        assertThat(node(reviewId, "main", 1)).isEqualTo(s1Before);
        assertThat(nodeDue(reviewId, "main", 1)).isEqualTo(s1Due);
        assertThat(jdbc.queryForList("SELECT metadata->>'reminded_nodes' FROM audit_events WHERE event_name = 'governance.supervisor_review.reminded'"
                + " AND business_id = ?", String.class, reviewId)).containsExactly((String) hrNode.get("node_id"));
        assertThat(reviewDue(reviewId)).isEqualTo(s1Due.isBefore(hrDue) ? s1Due : hrDue);
    }

    /** 员工薪资页「新增奖惩」：发起人给员工登记一笔奖励，返回复核单号。 */
    private String initiate(String tag) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/v1/staff/payroll/adjustments").with(session(initiator))
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "r246-adj-%s-%s", "user_id": "%s", "month": "2099-08", "adj_type": "bonus", "amount_cent": 100, "reason": "节点时限回归"}
                        """.formatted(tag, suffix, employee))).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8), "$.data.review_id");
    }

    /** 审批链页「通过」：奖惩复核要求填审批意见。 */
    private ResultActions approve(String reviewId, String approver, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + reviewId + "/approve").with(session(approver))
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "r246-approve-%s-%s", "decision": "approved", "comment": "同意"}
                        """.formatted(tag, suffix)));
    }

    private Map<String, Object> node(String reviewId, String lineCode, int seq) {
        return jdbc.queryForMap("SELECT node_id, approver_id, escalation_count FROM supervisor_review_nodes WHERE review_id = ? AND line_code = ? AND seq = ?",
                reviewId, lineCode, seq);
    }

    private Instant nodeDue(String reviewId, String lineCode, int seq) {
        Timestamp due = jdbc.queryForObject("SELECT due_at FROM supervisor_review_nodes WHERE review_id = ? AND line_code = ? AND seq = ?",
                Timestamp.class, reviewId, lineCode, seq);
        return due == null ? null : due.toInstant();
    }

    private Instant reviewDue(String reviewId) {
        return jdbc.queryForObject("SELECT due_at FROM supervisor_review_decisions WHERE review_id = ?", Timestamp.class, reviewId).toInstant();
    }

    /** 后台 Web 终端＋生产过滤器放入的 AccessActor 主体（四项与账号行一致）。 */
    private static RequestPostProcessor session(String userId) {
        AccessActor actor = new AccessActor(userId, userId, "staff", "project_main", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
