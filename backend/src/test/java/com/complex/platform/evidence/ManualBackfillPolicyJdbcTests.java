package com.complex.platform.evidence;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 F 段第 14 条 14-R1 人工补录参数与录音凭证上传，真库＋真实接口＋真实授权链，请求体按前端蛇形键。
 * 依据 BSM 第 1642 行「补录时限为业务发生后的配置天数内（安全阈值，按项目可配，调整须最高权限审批并留痕，出厂默认 3 天，须同时配置上下限）……
 * 后台配置的时长（业务参数，按项目可配，出厂默认 30 分钟，须同时配置上下限）」「补录必须附原始记录凭证（微信截图、通话录音等）」；
 * 决策台账 DEC-R3-2026-08-04-41「持授权 U 盘密钥的最高权限本人发起的操作不需要经过任何审批」。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ManualBackfillPolicyJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "prj_mbp_" + suffix;
    private final String otherProject = "prj_mbp_o_" + suffix;
    private RequestPostProcessor manager;
    private RequestPostProcessor merchantStaff;

    @BeforeEach
    void seed() {
        for (String p : List.of(project, otherProject)) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                    p, "补录参数项目 " + p);
        }
        String merchant = "mch_mbp_" + suffix;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                + "VALUES (?, '补录参数商户', 'bar', 'active', now(), now(), ?)", merchant, project);
        String role = "role_mbp_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'人工补录参数回归','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN("
                + "'evidence.manual_backfill.policy.manage','evidence.attachment.create')", role);
        manager = account("mgr", null, role);
        merchantStaff = account("mch", merchant, role);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 补录时限是安全阈值：非最高权限带时限组 → 403；只带录音提前量组 → 200，无自有行的项目时限沿用出厂 3 天。 */
    @Test
    void onlyMasterAdjustsTheWindowWhileTheRecordingLeadNeedsOnlyThePermission() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/manual-backfill-policies/" + project).with(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project_id").value("__global__"))
                .andExpect(jsonPath("$.data.window_days").value(3))
                .andExpect(jsonPath("$.data.recording_lead_minutes").value(30));
        save(manager, project, "w", "\"window_days\":5,\"window_min_days\":1,\"window_max_days\":10")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("MASTER_APPROVAL_REQUIRED"));
        save(manager, project, "l", "\"recording_lead_minutes\":45,\"lead_min_minutes\":10,\"lead_max_minutes\":120")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project_id").value(project))
                .andExpect(jsonPath("$.data.recording_lead_minutes").value(45))
                .andExpect(jsonPath("$.data.window_days").value(3));
        assertThat(jdbc.queryForMap("SELECT window_days, window_min_days, recording_lead_minutes, lead_max_minutes, updated_by "
                + "FROM manual_backfill_policies WHERE project_id=?", project))
                .containsEntry("window_days", 3).containsEntry("window_min_days", null)
                .containsEntry("recording_lead_minutes", 45).containsEntry("lead_max_minutes", 120)
                .containsEntry("updated_by", user("mgr"));
    }

    /** 组内三项须同填、值在上下限内（时限下限 ≥1，提前量下限 ≥0）；一组都不带也拒绝。 */
    @Test
    void eachGroupNeedsValueAndBoundsTogether() throws Exception {
        List<String> bodies = List.of("\"recording_lead_minutes\":45",
                "\"recording_lead_minutes\":45,\"lead_min_minutes\":50,\"lead_max_minutes\":120",
                "\"recording_lead_minutes\":45,\"lead_min_minutes\":-1,\"lead_max_minutes\":120",
                "\"lead_max_minutes\":120", "\"window_days\":null");
        for (int i = 0; i < bodies.size(); i++) {
            save(manager, project, "bad" + i, bodies.get(i))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        }
        save(master(), project, "w0", "\"window_days\":5,\"window_min_days\":0,\"window_max_days\":10")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM manual_backfill_policies WHERE project_id=?", Integer.class, project)).isZero();
    }

    /** 项目参数：商户账号 403；不可触及的项目 403；不存在的项目（含出厂行 __global__）不经接口改。 */
    @Test
    void merchantAccountsAndUnreachableProjectsAreRefused() throws Exception {
        save(merchantStaff, project, "m", "\"recording_lead_minutes\":45,\"lead_min_minutes\":10,\"lead_max_minutes\":120")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        save(manager, otherProject, "o", "\"recording_lead_minutes\":45,\"lead_min_minutes\":10,\"lead_max_minutes\":120")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        mockMvc.perform(get("/api/v1/evidence/manual-backfill-policies/" + otherProject).with(manager))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        save(master(), "__global__", "g", "\"window_days\":5,\"window_min_days\":1,\"window_max_days\":10")
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT window_days FROM manual_backfill_policies WHERE project_id='__global__'", Integer.class)).isEqualTo(3);
    }

    /** 最高权限本人改时限：留痕改动人与前后值；没带的录音提前量组沿用出厂值。 */
    @Test
    void masterAdjustsTheWindowWithBeforeAndAfterTrail() throws Exception {
        save(master(), project, "mw", "\"window_days\":5,\"window_min_days\":1,\"window_max_days\":10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.window_days").value(5))
                .andExpect(jsonPath("$.data.window_max_days").value(10))
                .andExpect(jsonPath("$.data.recording_lead_minutes").value(30))
                .andExpect(jsonPath("$.data.updated_by").value(SessionIssuer.MASTER_USER_ID));
        assertThat(jdbc.queryForMap("""
                SELECT metadata->'before'->>'window_days' AS before_days, metadata->'after'->>'window_days' AS after_days
                  FROM audit_events WHERE event_name='evidence.manual_backfill_policy_updated' AND business_id=?
                """, project)).containsEntry("before_days", "3").containsEntry("after_days", "5");
    }

    /** 录音凭证：business_type=manual_backfill 收 .m4a（服务端判 audio/mp4）；其他业务类型仍只收照片与视频。 */
    @Test
    void manualBackfillEvidenceAcceptsCallRecordings() throws Exception {
        mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", "call.m4a", "application/octet-stream", new byte[]{1, 2, 3}))
                        .param("business_type", "manual_backfill").with(merchantStaff))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content_type").value("audio/mp4"));
        mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", "call.m4a", "application/octet-stream", new byte[]{1, 2, 3}))
                        .param("business_type", "other").with(merchantStaff))
                .andExpect(status().isBadRequest());
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private ResultActions save(RequestPostProcessor who, String projectId, String key, String fields) throws Exception {
        return mockMvc.perform(put("/api/v1/evidence/manual-backfill-policies/" + projectId).with(who).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"mbp-" + key + "-" + suffix + "\",\"reason\":\"按项目调整\"," + fields + "}"));
    }

    private RequestPostProcessor account(String tag, String merchantId, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, project, merchantId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        return session(new AccessActor(userId, userId, "staff", project, merchantId, "active"));
    }

    private RequestPostProcessor master() {
        return session(new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                "platform-company", null, "active"));
    }

    private RequestPostProcessor session(AccessActor actor) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String user(String tag) {
        return "usr_mbp_" + tag + "_" + suffix;
    }
}
