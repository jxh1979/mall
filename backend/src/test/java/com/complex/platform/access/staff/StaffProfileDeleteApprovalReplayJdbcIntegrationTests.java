package com.complex.platform.access.staff;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.api.ApiResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BG 段第 261 条 261-B21-R4（真库、真实入口、蛇形键）。依据：PGM:528「⑤同一笔操作重复提交不重复生效（重复点击、网络重发、渠道重复回调都只算一次）」；
 * PGM:303「同一次操作重复提交时，返回第一次的结果并提示已完成。」。员工档案删除审批带请求编号：同一编号重发返回首次审批结果并提示已完成、
 * 成功留痕只一条；不带编号 400；驳回同样重放。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class StaffProfileDeleteApprovalReplayJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcStaffRepository repository;

    private final String s = UUID.randomUUID().toString().substring(0, 8);

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approvalResentWithSameKeyReturnsFirstDecisionOnce() throws Exception {
        String id = pendingDelete("ok");
        String body = "{\"idempotency_key\":\"b21r4-ok-" + s + "\",\"decision\":\"approved\"}";
        approve(id, body).andExpect(status().isOk()).andExpect(jsonPath("$.data.deleted").value(true))
                .andExpect(jsonPath("$.replayed").doesNotExist());
        approve(id, body).andExpect(status().isOk()).andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.message").value(ApiResponse.REPLAYED_MESSAGE))
                .andExpect(jsonPath("$.data.decision").value("approved")).andExpect(jsonPath("$.data.deleted").value(true));
        assertThat(events("staff.profile.delete_approved", id)).isEqualTo(1);
        assertThat(repository.findProfileById(id)).isEmpty();
    }

    @Test
    void rejectionResentWithSameKeyReturnsFirstDecisionOnce() throws Exception {
        String id = pendingDelete("no");
        String body = "{\"idempotency_key\":\"b21r4-no-" + s + "\",\"decision\":\"rejected\"}";
        approve(id, body).andExpect(status().isOk()).andExpect(jsonPath("$.data.deleted").value(false));
        approve(id, body).andExpect(status().isOk()).andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.data.decision").value("rejected"));
        assertThat(events("staff.profile.delete_rejected", id)).isEqualTo(1);
        assertThat(repository.findProfileById(id)).isPresent();
    }

    @Test
    void approvalWithoutKeyIsRejected() throws Exception {
        String id = pendingDelete("nk");
        approve(id, "{\"decision\":\"approved\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(repository.findProfileById(id)).isPresent();
        assertThat(events("staff.profile.delete_approved", id)).isZero();
    }

    // ---------- 夹具与请求 ----------

    /** 一份已提出删除申请（待审批）的员工档案。 */
    private String pendingDelete(String tag) {
        String id = "stf_b21r4_" + tag + "_" + s;
        Instant now = Instant.now();
        StaffProfileRecord profile = repository.saveProfile(new StaffProfileRecord(
                id, "proj_staff_test", null, "usr_b21r4_" + tag + "_" + s, "回归员工", "hash_p_" + tag + s, "hash_i_" + tag + s, null,
                "approved", "usr_admin", now, null, "active", now, now));
        repository.saveProfile(profile.withDelete("pending", "usr_admin", now, "离职", now));
        return id;
    }

    private ResultActions approve(String id, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/staff/profiles/" + id + "/delete-approve").with(master())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int events(String name, String id) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name=? AND business_id=?", Integer.class, name, id);
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
