package com.complex.platform.amusement;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.TemporaryGrant;
import com.complex.platform.common.api.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * M724-F11（治理地图第 303 行「同一次操作重复提交时，返回第一次的结果并提示已完成。」）：游乐事故关闭的真实库、真实接口回归。
 *
 * <p>接口 {@code POST /api/v1/amusement/incidents/{id}/close}（{@code @GuardedAction(admissionAction = "amusement.incident_close")}）。
 * 关闭成功后同一提交编号、同一正文重提：返回第一次的结果并提示已完成，关闭留痕只有一条、不新增复核单；同号改正文按请求编号冲突拒绝；
 * 换新编号再关已关闭的事故照旧被拒（{@code INCIDENT_CLOSED}）。修复前幂等层之前的「已关闭」校验先把重提拒掉（409 INCIDENT_CLOSED）。
 *
 * <p>V099 种子策略 {@code amusement_incident_close_over_space} 对带商户归属的员工按事故所在空间判定；本类的事故挂在不存在的
 * 游乐项目上、解析不到空间，恒按 fail-closed 拦截。放行单用真实拦截生成的待审复核单，再把它置为已批准
 * （同 {@code AdmissionReleaseOneShotJdbcIntegrationTests}）。
 * M724-F16（操作员 2026-10-07 裁决「批准只能被『被拦下的那次提交的同样内容』用掉，内容不同的另一笔照常拦截送审」）：
 * 被拦下的那次提交与之后用掉这张批准的那次提交用同一个处置结论。
 *
 * <p>本类不开测试事务：放行消费、幂等记录都要真实提交，重提才看得见。夹具按随机后缀隔离、结束时删除；
 * 复核单与审计事件只追加不删（V148 / V140），按后缀隔离留在库里。写请求带 {@code X-Terminal-Type: web}。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class AmusementIncidentCloseReplayJdbcIntegrationTests {

    private static final String CLOSE = "amusement.incident.close";
    private static final String ACTION = "amusement.incident_close";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccessRepository accessRepository;

    @Autowired
    private JdbcAmusementSafetyRepository repository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String userId = "usr_f11_" + suffix;
    private final String merchantId = "mch_f11_" + suffix;
    private final String incidentId = "ami_f11_" + suffix;

    @BeforeEach
    void seed() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM supervisor_review_policies
                 WHERE policy_code = 'amusement_incident_close_over_space' AND action_type = ?
                   AND enabled AND outcome = 'block' AND trigger_mode = 'over_space'
                """, Integer.class, ACTION)).as("V099 种子准入策略：事故关闭超空间拦截").isEqualTo(1);
        Instant now = Instant.now();
        accessRepository.createUser(new AccessUser(userId, userId, "staff", "org", merchantId, "active", now));
        accessRepository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), userId, List.of(CLOSE),
                now.minusSeconds(3600), now.plusSeconds(86_400), false, false, "active", "M724-F11 事故关闭重提回归",
                "seed-admin", now, null, null));
        // 本商户的一条待处理事故；所挂游乐项目不存在，准入闸解析不到空间
        repository.saveIncident(new AmusementIncidentRecord(
                incidentId,
                "prj_f11_" + suffix,
                null,
                null,
                null,
                null,
                "low",
                "accident",
                "created",
                "M724-F11 回归事故",
                "关闭成功后同号重提回归",
                userId,
                "staff_app",
                now,
                null,
                null,
                List.of(),
                now,
                now,
                merchantId
        ));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        // 处置记录随事故级联删除
        jdbcTemplate.update("DELETE FROM amusement_incidents WHERE id = ?", incidentId);
        jdbcTemplate.update("DELETE FROM temporary_permission_grants WHERE subject_id = ?", userId);
        jdbcTemplate.update("DELETE FROM access_users WHERE id = ?", userId);
        jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", "f11-%" + suffix);
    }

    @Test
    void closingAnIncidentAgainWithTheSameRequestReturnsTheFirstResultAndANewRequestOnTheClosedIncidentIsStillRejected()
            throws Exception {
        blockedThenApproved("f11-block-" + suffix, "已处置并回访");
        String key = "f11-close-" + suffix;
        String body = closeJson(key, "已处置并回访");

        String first = body(close(body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.replayed").doesNotExist())
                .andExpect(jsonPath("$.data.id").value(incidentId))
                .andExpect(jsonPath("$.data.status").value("closed")));
        assertThat(closedActions()).isEqualTo(1);
        assertThat(closedEvents()).isEqualTo(1);
        assertThat(reviews()).isEqualTo(1);

        // 同号同正文重提：返回第一次的结果并提示已完成（修复前 409 INCIDENT_CLOSED）；第二次还能过：再重提一次照样
        for (int attempt = 0; attempt < 2; attempt++) {
            String replayed = body(close(body)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(ApiResponse.REPLAYED_MESSAGE))
                    .andExpect(jsonPath("$.replayed").value(true)));
            assertThat(data(replayed)).isEqualTo(data(first));
        }
        // 关闭留痕只有一条、不新增复核单
        assertThat(closedActions()).isEqualTo(1);
        assertThat(closedEvents()).isEqualTo(1);
        assertThat(reviews()).isEqualTo(1);

        // 同号改正文：按请求编号冲突拒绝，不是「已完成」，不新增复核单
        close(closeJson(key, "改了处置结论"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("IDEMPOTENCY_CONFLICT"))
                .andExpect(jsonPath("$.replayed").doesNotExist());
        assertThat(reviews()).isEqualTo(1);

        // 换新编号再关已关闭的事故：第一张放行单已用掉，先再批一张（新的待审复核单置为已批准），凭它过准入闸后
        // 由幂等层之内的首次执行校验拒绝——INCIDENT_CLOSED
        blockedThenApproved("f11-block-again-" + suffix, "已处置并回访");
        close(closeJson("f11-close-again-" + suffix, "已处置并回访"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INCIDENT_CLOSED"))
                .andExpect(jsonPath("$.replayed").doesNotExist());
        assertThat(closedActions()).isEqualTo(1);
        assertThat(closedEvents()).isEqualTo(1);
    }

    /** 本人以给定处置结论发起一次被拦截的关闭（409 生成待审复核单），再把这张复核单置为已批准，返回其单号。 */
    private String blockedThenApproved(String key, String resolution) throws Exception {
        String response = body(close(closeJson(key, resolution))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED")));
        List<String> pending = jdbcTemplate.queryForList("""
                SELECT review_id FROM supervisor_review_decisions
                 WHERE action_type = ? AND object_id = ? AND initiated_by = ? AND review_status = 'pending_review'
                """, String.class, ACTION, incidentId, userId).stream().filter(response::contains).toList();
        assertThat(pending).hasSize(1);
        jdbcTemplate.update("""
                UPDATE supervisor_review_decisions
                   SET review_status = 'approved', decision = 'approved', reviewer_level = 3
                 WHERE review_id = ?
                """, pending.get(0));
        return pending.get(0);
    }

    private ResultActions close(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/amusement/incidents/" + incidentId + "/close")
                .with(request -> {
                    request.addHeader("X-Terminal-Type", "web");
                    return authentication(new UsernamePasswordAuthenticationToken(
                            new AccessActor(userId, userId, "staff", "org", merchantId, "active"), null,
                            List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String closeJson(String key, String resolution) {
        return """
                {"idempotency_key": "%s", "resolution": "%s", "closed_by_user_id": "%s"}
                """.formatted(key, resolution, userId);
    }

    private static String body(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // 响应包络的 request_id / server_time 每次不同，比较第一次与重提的业务结果只看 data
    private static JsonNode data(String body) throws Exception {
        return new ObjectMapper().readTree(body).get("data");
    }

    // 关闭留痕：事故处置记录里的「关闭」动作
    private Integer closedActions() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM amusement_incident_actions WHERE incident_id = ? AND action_type = 'closed'",
                Integer.class, incidentId);
    }

    // 关闭留痕：关闭成功的审计事件
    private Integer closedEvents() {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM audit_events
                 WHERE event_name = 'amusement.incident.closed' AND event_result = 'success' AND business_id = ?
                """, Integer.class, incidentId);
    }

    // 这条事故关闭动作名下的复核单总数（待审、已批准、已放行都算）
    private Integer reviews() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM supervisor_review_decisions WHERE action_type = ? AND object_id = ?",
                Integer.class, ACTION, incidentId);
    }
}
