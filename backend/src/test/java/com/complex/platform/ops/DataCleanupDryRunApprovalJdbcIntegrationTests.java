package com.complex.platform.ops;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
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
 * 第十八批 AE 段第 201 条 201-R7（契约补登集中数据清理四个入口）真实入口回归：按契约声明的蛇形请求体走真实 HTTP 与真实授权链，
 * 锁住试运行、提交、审批驳回的现行行为与拒绝路径。依据 BSM:576「选清理范围（已存档、且超过设定天数的配置数据）」
 * 「提交→由具备最高权限的人审批→批量清理，全程留痕、绝不碰在用的活数据」。入口已存在，回归只锁现行为（无 RED 阶段）。
 *
 * <p>夹具随测试事务回滚：发起人同时持 data.cleanup.approve，证明「不能审批自己的申请」由自审闸拦住而不是缺权限。</p>
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
    void submitNeedsReasonAndSelfApprovalIsForbiddenAndRejectionWritesNoCleanupEvent() throws Exception {
        String jobId = JsonPath.read(preview(requester, "{\"scope_type\":\"archived_bar_combos\",\"older_than_days\":0}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");

        submit(jobId, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        submit(jobId, "{\"reason\":\"清理测试遗留的旧套餐\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_approval"))
                .andExpect(jsonPath("$.data.reason").value("清理测试遗留的旧套餐"))
                .andExpect(jsonPath("$.data.requested_by").value(requesterId));
        submit(jobId, "{\"reason\":\"再次提交\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DATA_CLEANUP_NOT_DRY_RUN"));

        approve(requester, jobId, "approved")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("DATA_CLEANUP_SELF_APPROVE_FORBIDDEN"));
        approve(approver, jobId, "maybe")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(statusOf(jobId)).isEqualTo("pending_approval");

        approve(approver, jobId, " Rejected ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("rejected"))
                .andExpect(jsonPath("$.data.approved_by").value(approverId))
                .andExpect(jsonPath("$.data.executed_at").doesNotExist());
        approve(approver, jobId, "approved")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DATA_CLEANUP_NOT_PENDING"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name LIKE 'data.cleanup.%' AND business_id = ?",
                Integer.class, jobId)).isZero();
    }

    // ---------- helpers ----------

    private ResultActions preview(RequestPostProcessor who, String json) throws Exception {
        return mockMvc.perform(post("/api/v1/ops/data-cleanup/preview").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions submit(String jobId, String json) throws Exception {
        return mockMvc.perform(post("/api/v1/ops/data-cleanup/jobs/" + jobId + "/submit").with(requester)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions approve(RequestPostProcessor who, String jobId, String decision) throws Exception {
        return mockMvc.perform(post("/api/v1/ops/data-cleanup/jobs/" + jobId + "/approve").with(who)
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"" + decision + "\"}"));
    }

    private String statusOf(String jobId) {
        return jdbc.queryForObject("SELECT status FROM data_cleanup_jobs WHERE id = ?", String.class, jobId);
    }
}
