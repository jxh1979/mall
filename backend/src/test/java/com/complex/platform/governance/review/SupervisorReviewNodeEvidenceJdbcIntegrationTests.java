package com.complex.platform.governance.review;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
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
 * 第十八批 BA 段第 353 条 353-R1（PGM:899「谁发起、谁销售、谁服务、谁第一个处理异常、为什么、谁审批、结果是什么，都要能查到。」；PGM:528「系统按组织
 * 层级自动补全从发起人到该最终审批人之间的全部直接上级节点，逐级审批」）：复核链上每个节点审批人所附凭证都并入复核单凭证（去重保序），并记入该节点的
 * governance.supervisor_review.node_decided 留痕；终审后复核单凭证依次为提交时凭证与各节点所附。真库＋真实接口＋真实角色授权链，请求体按主管复核页
 * 蛇形键。
 *
 * <p>组织层级照 246-R1 夹具：员工 E → S1 → S2 → S3 → S4；部门人事终审人 HR；发起人 I 不在链上。奖惩复核生成主线 S1、S2、S3 与 hr_review 会签线。
 * 夹具随测试事务回滚。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SupervisorReviewNodeEvidenceJdbcIntegrationTests {

    @Autowired private AccessRepository accessRepository;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String suffix = Long.toString(System.nanoTime());
    private String employee;
    private String initiator;
    private String hr;
    private List<String> superiors;

    @BeforeEach
    void seed() {
        employee = "usr_r353_e_" + suffix;
        initiator = "usr_r353_i_" + suffix;
        hr = "usr_r353_hr_" + suffix;
        superiors = List.of("usr_r353_s1_" + suffix, "usr_r353_s2_" + suffix, "usr_r353_s3_" + suffix, "usr_r353_s4_" + suffix);
        String role = "role_r353_" + suffix;
        jdbc.update("INSERT INTO roles(id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '节点凭证回归', 'platform', 'active', now())",
                role, "节点凭证回归" + suffix);
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
        jdbc.update("INSERT INTO approval_lines(line_code, line_name, min_start_rank, status, hr_final_approver_id) VALUES (?, '节点凭证回归部门', 1, 'active', ?)",
                "dept_r353_" + suffix, hr);
        jdbc.update("UPDATE org_hierarchy SET line_code = ? WHERE user_id = ?", "dept_r353_" + suffix, employee);
        jdbc.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id,
                        registration_status, status, employment_status, created_at, updated_at)
                VALUES (?, 'project_main', NULL, ?, '节点凭证员工', 'project_main', 'approved', 'active', 'employed', now(), now())
                """, "stf_r353_" + suffix, employee);
    }

    /** S1、S2 是中间节点，各附一份凭证；S3 与 HR 不附凭证通过后整单通过，复核单凭证依次为提交时凭证、S1 所附、S2 所附。 */
    @Test
    void everyNodeEvidenceIsMergedIntoReviewAndTracedOnItsNode() throws Exception {
        String reviewId = initiate();
        List<String> submitted = reviewEvidence(reviewId);
        String attA = "att_r353_a_" + suffix;
        String attB = "att_r353_b_" + suffix;

        approve(reviewId, superiors.get(0), "s1", List.of(attA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("pending_review"))
                .andExpect(jsonPath("$.data.evidence_attachment_ids").value(hasItem(attA)));
        assertThat(reviewEvidence(reviewId)).containsExactlyElementsOf(concat(submitted, attA));
        assertThat(nodeEvidence(reviewId, 1)).containsExactly(attA);

        approve(reviewId, superiors.get(1), "s2", List.of(attB)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("pending_review"));
        assertThat(reviewEvidence(reviewId)).containsExactlyElementsOf(concat(submitted, attA, attB));
        assertThat(nodeEvidence(reviewId, 2)).containsExactly(attB);

        approve(reviewId, superiors.get(2), "s3", List.of()).andExpect(status().isOk());
        assertThat(nodeEvidence(reviewId, 3)).isEmpty();
        approve(reviewId, hr, "hr", List.of()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("approved"));
        assertThat(reviewEvidence(reviewId)).containsExactlyElementsOf(concat(submitted, attA, attB));
    }

    /** 员工薪资页「新增奖惩」：发起人给员工登记一笔奖励，返回复核单号。 */
    private String initiate() throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/v1/staff/payroll/adjustments").with(session(initiator))
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "r353-adj-%s", "user_id": "%s", "month": "2099-08", "adj_type": "bonus", "amount_cent": 100, "reason": "节点凭证回归"}
                        """.formatted(suffix, employee))).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8), "$.data.review_id");
    }

    /** 主管复核页、审批链页「通过」：奖惩复核要求填审批意见，证据附件编号框按逗号拆成数组。 */
    private ResultActions approve(String reviewId, String approver, String tag, List<String> evidence) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + reviewId + "/approve").with(session(approver))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "idempotency_key", "r353-approve-" + tag + "-" + suffix, "decision", "approved", "comment", "核对无误",
                        "operator_note", "完整后台主管审批放行", "evidence_attachment_ids", evidence))));
    }

    private List<String> reviewEvidence(String reviewId) {
        return jdbc.queryForList("SELECT jsonb_array_elements_text(evidence_attachment_ids) FROM supervisor_review_decisions WHERE review_id = ?",
                String.class, reviewId);
    }

    /** 主线第 seq 个节点裁决留痕里记的本节点审批人所附凭证。 */
    private List<String> nodeEvidence(String reviewId, int seq) {
        return jdbc.queryForList("""
                SELECT jsonb_array_elements_text(metadata->'evidence_attachment_ids') FROM audit_events
                 WHERE event_name = 'governance.supervisor_review.node_decided' AND business_id = ?
                   AND metadata->>'line_code' = 'main' AND metadata->>'seq' = ?
                """, String.class, reviewId, String.valueOf(seq));
    }

    private static List<String> concat(List<String> head, String... tail) {
        List<String> all = new ArrayList<>(head);
        all.addAll(List.of(tail));
        return all;
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
