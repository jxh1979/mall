package com.complex.platform.ops;

// 文件版本：V0.04

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
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
 * 第十八批 AE 段第 201 条 201-R7（契约补登集中数据清理四个入口）真实入口回归：按契约声明的蛇形请求体走真实 HTTP 与真实授权链，
 * 锁住试运行、提交、审批驳回的现行行为与拒绝路径。依据 BSM:576「选清理范围（已存档、且超过设定天数的配置数据）」
 * 「提交→由具备最高权限的人审批→批量清理，全程留痕、绝不碰在用的活数据」。入口已存在，回归只锁现行为（无 RED 阶段）。
 *
 * <p>夹具随测试事务回滚：发起人同时持 data.cleanup.approve。第十八批 AO 段第 249 条 249-R7（BSM:576「删除一律须由具备最高权限的人
 * 审批通过后执行，不设例外……持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」）：审批只认最高权限本人，发起人与另一位
 * 持 data.cleanup.approve 的非最高权限审批人都 403 MASTER_APPROVAL_REQUIRED；最高权限本人提交即执行。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class DataCleanupDryRunApprovalJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = String.valueOf(System.nanoTime());
    private final String requesterId = "usr_dcl_req_" + s;
    private final String approverId = "usr_dcl_apr_" + s;
    private RequestPostProcessor requester;
    private RequestPostProcessor approver;
    private RequestPostProcessor outsider;

    @BeforeEach
    void setUp() {
        String project = "dcl_prj_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','数据清理综合体','active',now(),now())", project);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        requester = staffs.staff(requesterId, "staff", project, null, "data.cleanup.request", "data.cleanup.approve");
        approver = staffs.staff(approverId, "staff", project, null, "data.cleanup.approve");
        outsider = staffs.staff("usr_dcl_out_" + s, "staff", project, null, "ops.production_readiness.view");
    }

    @Test
    void dryRunWithContractBodyIsListedAndOutOfRangeOrUnauthorizedPreviewIsRejected() throws Exception {
        String body = preview(requester, "{\"scope_type\":\"archived_bar_products\",\"older_than_days\":30}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("dry_run"))
                .andExpect(jsonPath("$.data.scope_type").value("archived_bar_products"))
                .andExpect(jsonPath("$.data.requested_by").value(requesterId))
                .andExpect(jsonPath("$.data.preview_count").isNumber())
                .andExpect(jsonPath("$.data.preview_sample").isArray())
                .andReturn().getResponse().getContentAsString();
        String jobId = JsonPath.read(body, "$.data.id");

        String listed = mockMvc.perform(get("/api/v1/ops/data-cleanup/jobs").param("status", "dry_run").with(requester))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(listed, "$.data[*].id");
        List<String> statuses = JsonPath.read(listed, "$.data[*].status");
        assertThat(ids).contains(jobId);
        assertThat(statuses).containsOnly("dry_run");

        preview(requester, "{\"scope_type\":\"archived_bar_products\",\"older_than_days\":3651}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        preview(requester, "{\"scope_type\":\"active_bar_products\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        preview(outsider, "{\"scope_type\":\"archived_bar_products\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    @Test
    void submitNeedsReasonAndSelfApprovalIsForbiddenAndRejectionIsTraced() throws Exception {
        String jobId = JsonPath.read(preview(requester, "{\"scope_type\":\"archived_bar_combos\",\"older_than_days\":0}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");

        submit(requester, jobId, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        submit(requester, jobId, "{\"reason\":\"清理测试遗留的旧套餐\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_approval"))
                .andExpect(jsonPath("$.data.reason").value("清理测试遗留的旧套餐"))
                .andExpect(jsonPath("$.data.requested_by").value(requesterId));
        submit(requester, jobId, "{\"reason\":\"再次提交\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DATA_CLEANUP_NOT_DRY_RUN"));

        approve(requester, jobId, "approved")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MASTER_APPROVAL_REQUIRED"));
        approve(approver, jobId, "approved")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MASTER_APPROVAL_REQUIRED"));
        approve(master(), jobId, "maybe")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(statusOf(jobId)).isEqualTo("pending_approval");

        approve(master(), jobId, " Rejected ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("rejected"))
                .andExpect(jsonPath("$.data.approved_by").value(SessionIssuer.MASTER_USER_ID))
                .andExpect(jsonPath("$.data.executed_at").doesNotExist());
        approve(master(), jobId, "approved")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DATA_CLEANUP_NOT_PENDING"));
        // 第十八批 BA 段第 275 条 275-R1：提交与驳回各落一条，驳回不执行清理。
        assertThat(jdbc.queryForList("SELECT event_name FROM audit_events WHERE event_name LIKE 'data.cleanup.%' AND business_id = ?",
                String.class, jobId)).containsExactlyInAnyOrder("data.cleanup.submitted", "data.cleanup.rejected");
    }

    /**
     * 第十八批 BA 段第 275 条 275-R1（BSM:576「提交→由具备最高权限的人审批→批量清理，全程留痕」；PGM:966「所有有过程的业务动作都必须留痕」；
     * PGM:899「谁发起……谁审批、结果是什么，都要能查到。」）：换人提交时提交人与事由进留痕；最高权限驳回落驳回事件；最高权限本人提交依次落
     * 提交与执行；对已提交的任务再提交落提交失败；缺事由仍是 400 不是 500。
     */
    @Test
    void submitAndRejectionWriteTraceEvents() throws Exception {
        String submitterId = "usr_dcl_sub_" + s;
        RequestPostProcessor submitter = new StaffRequestFixture(jdbc).staff(submitterId, "staff", "dcl_prj_" + s, null, "data.cleanup.request");
        String jobId = JsonPath.read(preview(requester, "{\"scope_type\":\"archived_bar_combos\",\"older_than_days\":0}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");

        submit(submitter, jobId, "{\"reason\":\"清理测试遗留\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_approval"));
        assertThat(jdbc.queryForList("SELECT actor_id, metadata->>'reason' AS reason FROM audit_events"
                + " WHERE event_name = 'data.cleanup.submitted' AND business_id = ?", jobId))
                .containsExactly(Map.of("actor_id", submitterId, "reason", "清理测试遗留"));

        int failedBefore = submitFailures();
        submit(submitter, jobId, "{\"reason\":\"再次提交\"}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DATA_CLEANUP_NOT_DRY_RUN"));
        assertThat(submitFailures()).isEqualTo(failedBefore + 1);

        approve(master(), jobId, "rejected").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("rejected"));
        assertThat(jdbc.queryForList("SELECT actor_id FROM audit_events WHERE event_name = 'data.cleanup.rejected' AND business_id = ?",
                String.class, jobId)).containsExactly(SessionIssuer.MASTER_USER_ID);

        String masterJob = JsonPath.read(preview(master(), "{\"scope_type\":\"archived_bar_combos\",\"older_than_days\":0}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        submit(master(), masterJob, "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        submit(master(), masterJob, "{\"reason\":\"最高权限清理旧套餐\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("executed"));
        assertThat(jdbc.queryForList("SELECT event_name FROM audit_events WHERE event_name LIKE 'data.cleanup.%' AND business_id = ?",
                String.class, masterJob)).containsExactlyInAnyOrder("data.cleanup.submitted", "data.cleanup.executed");
    }

    /** 249-R7：最高权限本人审批通过即执行，落 data.cleanup.executed。 */
    @Test
    void masterApprovalExecutesTheCleanup() throws Exception {
        String jobId = JsonPath.read(preview(requester, "{\"scope_type\":\"archived_bar_combos\",\"older_than_days\":0}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        submit(requester, jobId, "{\"reason\":\"清理测试遗留的旧套餐\"}").andExpect(status().isOk());

        approve(master(), jobId, "approved")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("executed"))
                .andExpect(jsonPath("$.data.approved_by").value(SessionIssuer.MASTER_USER_ID));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'data.cleanup.executed' AND business_id = ?",
                Integer.class, jobId)).isEqualTo(1);
    }

    /** 249-R7：最高权限本人提交试运行任务即执行，不经审批（BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」）。 */
    @Test
    void masterSubmissionExecutesWithoutApproval() throws Exception {
        String jobId = JsonPath.read(preview(master(), "{\"scope_type\":\"archived_bar_combos\",\"older_than_days\":0}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");

        submit(master(), jobId, "{\"reason\":\"最高权限清理旧套餐\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("executed"))
                .andExpect(jsonPath("$.data.reason").value("最高权限清理旧套餐"))
                .andExpect(jsonPath("$.data.approved_by").value(SessionIssuer.MASTER_USER_ID));
        assertThat(statusOf(jobId)).isEqualTo("executed");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'data.cleanup.executed' AND business_id = ?",
                Integer.class, jobId)).isEqualTo(1);
    }

    /**
     * 第十八批 BG 段第 261 条 261-B21-R3（PGM:528「⑤同一笔操作重复提交不重复生效」；PGM:303「同一次操作重复提交时，返回第一次的结果并提示已完成。」）：
     * 提交审批同一请求编号连发两次，第二次返回首次结果并提示已完成、提交留痕只一条；最高权限批准同一编号连发两次，第二次返回首次结果，
     * 清理没有再执行一遍（执行时间、清理条数与第一次相同，执行留痕只一条）。修复前两处第二次都 409。
     */
    @Test
    void submitAndApprovalResentWithSameKeyReturnFirstResult() throws Exception {
        String jobId = JsonPath.read(preview(requester, "{\"scope_type\":\"archived_bar_combos\",\"older_than_days\":0}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        String submitUrl = "/api/v1/ops/data-cleanup/jobs/" + jobId + "/submit";
        String submitBody = "{\"idempotency_key\":\"b21r3-sub-" + s + "\",\"reason\":\"清理测试遗留的旧套餐\"}";
        raw(requester, submitUrl, submitBody).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_approval")).andExpect(jsonPath("$.replayed").doesNotExist());
        raw(requester, submitUrl, submitBody).andExpect(status().isOk()).andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.message").value(com.complex.platform.common.api.ApiResponse.REPLAYED_MESSAGE))
                .andExpect(jsonPath("$.data.status").value("pending_approval"));
        assertThat(events("data.cleanup.submitted", jobId)).isEqualTo(1);

        String approveUrl = "/api/v1/ops/data-cleanup/jobs/" + jobId + "/approve";
        String approveBody = "{\"idempotency_key\":\"b21r3-apr-" + s + "\",\"decision\":\"approved\"}";
        String first = raw(master(), approveUrl, approveBody).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("executed")).andReturn().getResponse().getContentAsString();
        String second = raw(master(), approveUrl, approveBody).andExpect(status().isOk()).andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.data.status").value("executed")).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<Object>read(second, "$.data.executed_at")).isEqualTo(JsonPath.<Object>read(first, "$.data.executed_at"));
        assertThat(JsonPath.<Object>read(second, "$.data.deleted_count")).isEqualTo(JsonPath.<Object>read(first, "$.data.deleted_count"));
        assertThat(events("data.cleanup.executed", jobId)).isEqualTo(1);
    }

    /** 261-B21-R3：提交、审批不带请求编号都 400、任务状态不变；驳回同一编号重发返回首次结果、驳回留痕只一条。 */
    @Test
    void missingKeyIsRejectedAndRejectionReplays() throws Exception {
        String jobId = JsonPath.read(preview(requester, "{\"scope_type\":\"archived_bar_combos\",\"older_than_days\":0}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        raw(requester, "/api/v1/ops/data-cleanup/jobs/" + jobId + "/submit", "{\"reason\":\"清理测试遗留的旧套餐\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(statusOf(jobId)).isEqualTo("dry_run");
        submit(requester, jobId, "{\"reason\":\"清理测试遗留的旧套餐\"}").andExpect(status().isOk());

        String approveUrl = "/api/v1/ops/data-cleanup/jobs/" + jobId + "/approve";
        raw(master(), approveUrl, "{\"decision\":\"rejected\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(statusOf(jobId)).isEqualTo("pending_approval");
        String rejectBody = "{\"idempotency_key\":\"b21r3-rej-" + s + "\",\"decision\":\"rejected\"}";
        raw(master(), approveUrl, rejectBody).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("rejected"));
        raw(master(), approveUrl, rejectBody).andExpect(status().isOk()).andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.data.status").value("rejected"));
        assertThat(events("data.cleanup.rejected", jobId)).isEqualTo(1);
    }

    // ---------- helpers ----------

    private int events(String name, String jobId) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = ? AND business_id = ?", Integer.class, name, jobId);
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

    private ResultActions preview(RequestPostProcessor who, String json) throws Exception {
        return mockMvc.perform(post("/api/v1/ops/data-cleanup/preview").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** 每次调用是一次新的提交：自动带一个新的请求编号（261-B21-R3 起接口必填）。 */
    private ResultActions submit(RequestPostProcessor who, String jobId, String json) throws Exception {
        String keyed = "{\"idempotency_key\":\"dcl-" + java.util.UUID.randomUUID() + "\"" + ("{}".equals(json) ? "}" : "," + json.substring(1));
        return raw(who, "/api/v1/ops/data-cleanup/jobs/" + jobId + "/submit", keyed);
    }

    private ResultActions approve(RequestPostProcessor who, String jobId, String decision) throws Exception {
        return raw(who, "/api/v1/ops/data-cleanup/jobs/" + jobId + "/approve",
                "{\"idempotency_key\":\"dcl-" + java.util.UUID.randomUUID() + "\",\"decision\":\"" + decision + "\"}");
    }

    private ResultActions raw(RequestPostProcessor who, String url, String json) throws Exception {
        return mockMvc.perform(post(url).with(who).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private int submitFailures() {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'data.cleanup.submit_failed'", Integer.class);
    }

    private String statusOf(String jobId) {
        return jdbc.queryForObject("SELECT status FROM data_cleanup_jobs WHERE id = ?", String.class, jobId);
    }
}
