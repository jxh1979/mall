package com.complex.platform.governance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 G 段第 82 条 82b-R5（单据类本人项目口径改按入口权限码可触及项目）真库＋真实接口＋真实角色勾选回归，请求体一律按前端蛇形键。
 * 依据：治理地图第 659 行「被分配了可跨项目角色的员工（含经岗位得到），在所分配的权限内可以查看、处理该角色可查看项目的数据。」
 * 「角色能查看哪些项目，持有这类角色的员工就能在哪些项目新建记录」；第 528 行「不受第④类「别的项目」限制」。
 *
 * <p>夹具随测试事务回滚：项目 A、B 两行（编号带随机后缀），操作人都是归属 A 的员工；持有人的角色只逐个勾 B，普通员工的角色两项都没勾。
 * 被拒请求的失败留痕是独立事务，按随机后缀隔离留在库里。终端设备一项由设备线另做，不在本类。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class CrossProjectBusinessRecordsJdbcIntegrationTests {

    @Autowired private AccessRepository accessRepository;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_g82r5_a_" + suffix;
    private final String projectB = "prj_g82r5_b_" + suffix;

    @BeforeEach
    void createProjects() {
        for (String projectId : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', ?, 'active', now(), now())", projectId, "G82R5 " + projectId);
        }
    }

    // ① 全园故障：持有人不带项目与带 B 都列出 B 的故障、能以游乐公司负责人确认生效；普通员工带 B 列表与确认都 403；补偿批次按事件列表同口径。
    @Test
    void parkWideFaultsAndCompensationBatchesFollowTheRole() throws Exception {
        String faultId = "apwf_g82r5_" + suffix;
        jdbc.update("INSERT INTO amusement_park_wide_faults (id, project_id, status, reason, initiated_by, initiated_role, initiated_at) "
                + "VALUES (?, ?, 'pending_activation', 'G82R5 全部游乐项目不可运行', ?, 'engineering_safety', now())",
                faultId, projectB, "usr_g82r5_init_" + suffix);
        String[] codes = {"amusement.park_fault.view", "amusement.park_fault.confirm_as_head"};
        RequestPostProcessor ticked = staffOfA("pf", role("pf", List.of(projectB), codes));
        RequestPostProcessor plain = staffOfA("pfp", role("pfp", List.of(), codes));
        String faults = "/api/v1/amusement/park-wide-faults";
        String confirm = faults + "/" + faultId + "/confirm-activation";
        String batches = "/api/v1/amusement/compensation-batches?project_id=" + projectB;

        mockMvc.perform(get(faults + "?project_id=" + projectB).with(plain))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        send(plain, post(confirm), "{\"role\":\"amusement_head\",\"idempotency_key\":\"f0-" + suffix + "\"}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        mockMvc.perform(get(batches).with(plain))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));

        assertThat(ids(ticked, faults, "$.data.items[*].id")).contains(faultId);
        assertThat(ids(ticked, faults + "?project_id=" + projectB, "$.data.items[*].id")).contains(faultId);
        send(ticked, post(confirm), "{\"role\":\"amusement_head\",\"idempotency_key\":\"f1-" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("active"));
        mockMvc.perform(get(faults + "/" + faultId + "/actions").with(ticked)).andExpect(status().isOk());
        mockMvc.perform(get(batches).with(ticked)).andExpect(status().isOk());
    }

    // ② 演出场次：持有人列表（不带项目）见 B 的场次、能看详情；普通员工看详情与带 B 列表都 403。
    @Test
    void mediaShowsFollowTheRole() throws Exception {
        String showId = "ms_g82r5_" + suffix;
        jdbc.update("INSERT INTO media_performance_shows (id, show_group_id, project_id, media_merchant_id, show_kind, show_date, created_by) "
                + "VALUES (?, ?, ?, ?, 'self_hosted', current_date, 'usr_g82r5')", showId, showId, projectB, "mer_g82r5_media_" + suffix);
        RequestPostProcessor ticked = staffOfA("ms", role("ms", List.of(projectB), "media.performance.view"));
        RequestPostProcessor plain = staffOfA("msp", role("msp", List.of(), "media.performance.view"));
        String shows = "/api/v1/settlement/media-shows";

        mockMvc.perform(get(shows + "/" + showId).with(plain))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
        mockMvc.perform(get(shows + "?project_id=" + projectB).with(plain))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));

        assertThat(ids(ticked, shows, "$.data[*].id")).contains(showId);
        mockMvc.perform(get(shows + "/" + showId).with(ticked)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.show.id").value(showId));
    }

    // ①b 补偿批次确认的范围闸在幂等之前：持有人以游乐公司负责人确认 B 的批次成功后角色去掉 B，同一幂等键重放 403、不返回第一次结果。
    // 复核修复前红：重放 200。
    @Test
    void compensationBatchConfirmGateRunsBeforeIdempotency() throws Exception {
        String batchId = "afcb_g82r5_" + suffix;
        jdbc.update("INSERT INTO amusement_fault_compensation_batches (id, project_id, status, reason, bearer_company_id, validity_days, "
                + "window_from, window_to, event_type) VALUES (?, ?, 'pending_confirmation', 'G82R5 安全事件恢复', 'ent_g82r5', 365, now(), now(), "
                + "'safety_restore')", batchId, projectB);
        String role = role("cb", List.of(projectB), "amusement.park_fault.view", "amusement.park_fault.confirm_as_head");
        RequestPostProcessor ticked = staffOfA("cb", role);
        String url = "/api/v1/amusement/compensation-batches/" + batchId + "/confirm";
        String body = "{\"role\":\"amusement_head\",\"idempotency_key\":\"cb1-" + suffix + "\"}";

        send(ticked, post(url), body).andExpect(status().isOk());
        jdbc.update("DELETE FROM role_viewable_projects WHERE role_id=?", role);
        send(ticked, post(url), body)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
    }

    // ②b 演出场次锁定与结算的范围闸在幂等之前：持有人锁定、结算 B 的场次成功后角色去掉 B，同一幂等键重放 403、不返回第一次结果。
    // 复核修复前红：重放 200。
    @Test
    void mediaShowLockAndSettleGatesRunBeforeIdempotency() throws Exception {
        String draft = "ms_g82r5_d_" + suffix;
        String locked = "ms_g82r5_l_" + suffix;
        for (String[] show : List.of(new String[] {draft, "draft"}, new String[] {locked, "locked"})) {
            jdbc.update("INSERT INTO media_performance_shows (id, show_group_id, project_id, media_merchant_id, show_kind, show_date, "
                    + "created_by, status, locked_at, locked_by) VALUES (?, ?, ?, ?, 'self_hosted', current_date, 'usr_g82r5', ?, "
                    + "CASE WHEN ? = 'locked' THEN now() END, CASE WHEN ? = 'locked' THEN 'usr_g82r5' END)",
                    show[0], show[0], projectB, "mer_g82r5_media_" + suffix, show[1], show[1], show[1]);
        }
        String role = role("ml", List.of(projectB), "media.performance.contract.manage", "media.performance.settle");
        RequestPostProcessor ticked = staffOfA("ml", role);
        String shows = "/api/v1/settlement/media-shows/";
        String lock = "{\"idempotency_key\":\"ml1-" + suffix + "\"}";
        String settle = "{\"idempotency_key\":\"ms1-" + suffix + "\"}";

        send(ticked, post(shows + draft + "/lock"), lock).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("locked"));
        send(ticked, post(shows + locked + "/settle"), settle).andExpect(status().isOk());
        jdbc.update("DELETE FROM role_viewable_projects WHERE role_id=?", role);
        send(ticked, post(shows + draft + "/lock"), lock)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
        send(ticked, post(shows + locked + "/settle"), settle)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
    }

    // ④ 企微会话脱敏调阅：持有人调阅 B 的会话 200 并留调阅痕；普通员工 403 CROSS_PROJECT_FORBIDDEN、不留调阅痕。
    @Test
    void wecomConversationMessagesFollowTheRole() throws Exception {
        String conversationId = "wconv_g82r5_" + suffix;
        jdbc.update("INSERT INTO wecom_conversations (id, external_conversation_id, project_id) VALUES (?, ?, ?)",
                conversationId, "ext_g82r5_" + suffix, projectB);
        String url = "/api/v1/compliance/wecom/conversations/" + conversationId + "/messages?mode=redacted";
        RequestPostProcessor plain = staffOfA("wc", role("wc", List.of(), "wecom.view"));
        mockMvc.perform(get(url).with(plain))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM wecom_access_audits WHERE conversation_id=?", Integer.class, conversationId))
                .isZero();

        RequestPostProcessor ticked = staffOfA("wct", role("wct", List.of(projectB), "wecom.view"));
        mockMvc.perform(get(url).with(ticked)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM wecom_access_audits WHERE conversation_id=?", Integer.class, conversationId))
                .isEqualTo(1);
    }

    // ⑥ 经销商：持有人建档带 B 落 B、带未勾的本人项目 A 403；列表只见 B 与项目为空的经销商；能读 B 经销商的合同。普通员工带 B 建档、读 B 经销商都 403。
    @Test
    void dealersLandAndListByTheRole() throws Exception {
        String dealerB = "dlr_g82r5_b_" + suffix;
        String dealerA = "dlr_g82r5_a_" + suffix;
        String dealerNone = "dlr_g82r5_n_" + suffix;
        jdbc.update("INSERT INTO dealers (id, project_id, dealer_name) VALUES (?, ?, 'G82R5 B'), (?, ?, 'G82R5 A'), (?, NULL, 'G82R5 无项目')",
                dealerB, projectB, dealerA, projectA, dealerNone);
        String[] codes = {"dealer.view", "dealer.manage"};
        RequestPostProcessor ticked = staffOfA("dl", role("dl", List.of(projectB), codes));
        RequestPostProcessor plain = staffOfA("dlp", role("dlp", List.of(), codes));

        send(plain, post("/api/v1/dealers"), "{\"dealer_name\":\"G82R5 普通 " + suffix + "\",\"project_id\":\"" + projectB
                + "\",\"idempotency_key\":\"d0-" + suffix + "\"}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        mockMvc.perform(get("/api/v1/dealers/" + dealerB + "/contracts").with(plain))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"));

        send(ticked, post("/api/v1/dealers"), "{\"dealer_name\":\"G82R5 持有 " + suffix + "\",\"project_id\":\"" + projectB
                + "\",\"idempotency_key\":\"d1-" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));
        send(ticked, post("/api/v1/dealers"), "{\"dealer_name\":\"G82R5 持有本人 " + suffix + "\",\"project_id\":\"" + projectA
                + "\",\"idempotency_key\":\"d2-" + suffix + "\"}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        List<String> listed = ids(ticked, "/api/v1/dealers", "$.data.items[*].id");
        assertThat(listed).contains(dealerB, dealerNone).doesNotContain(dealerA);
        mockMvc.perform(get("/api/v1/dealers/" + dealerB + "/contracts").with(ticked)).andExpect(status().isOk());
    }

    // ⑦ 员工车辆登记：持有人给 B 的员工登记成功；普通员工 403。
    @Test
    void staffVehicleRegistrationFollowsTheRole() throws Exception {
        String staffB = "usr_g82r5_vb_" + suffix;
        accessRepository.createUser(new AccessUser(staffB, staffB, "staff", projectB, null, "active", Instant.now()));
        String url = "/api/v1/parking/staff-vehicles";
        RequestPostProcessor plain = staffOfA("pv", role("pv", List.of(), "parking.vehicle.manage"));
        send(plain, post(url), "{\"project_id\":\"" + projectB + "\",\"user_id\":\"" + staffB + "\",\"plate_no\":\"G8P" + suffix
                + "\",\"idempotency_key\":\"v0-" + suffix + "\"}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));

        RequestPostProcessor ticked = staffOfA("pvt", role("pvt", List.of(projectB), "parking.vehicle.manage"));
        send(ticked, post(url), "{\"project_id\":\"" + projectB + "\",\"user_id\":\"" + staffB + "\",\"plate_no\":\"G8T" + suffix
                + "\",\"idempotency_key\":\"v1-" + suffix + "\"}")
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_staff_vehicles WHERE project_id=? AND user_id=?", Integer.class,
                projectB, staffB)).isEqualTo(1);
    }

    // ── 角色、会话与取数 ─────────────────────────────────────────────────────────────────────────

    private String role(String tag, List<String> ticked, String... codes) {
        String roleId = "role_g82r5_" + tag + "_" + suffix;
        accessRepository.createRole(new RoleRecord(roleId, "G82R5 " + tag + " " + suffix, null, "project", "active", Instant.now(),
                List.of(codes), null, false, ticked));
        return roleId;
    }

    /** 建一名归属 A 的员工、挂上给定角色，返回以其身份登录的会话（后台 Web 终端＋生产过滤器放入的 AccessActor 主体）。 */
    private RequestPostProcessor staffOfA(String tag, String roleId) {
        String userId = "usr_g82r5_" + tag + "_" + suffix;
        accessRepository.createUser(new AccessUser(userId, userId, "staff", projectA, null, "active", Instant.now()));
        accessRepository.assignUserRole(userId, roleId);
        AccessActor actor = new AccessActor(userId, userId, "staff", projectA, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private ResultActions send(RequestPostProcessor session, MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.with(session).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private List<String> ids(RequestPostProcessor session, String url, String path) throws Exception {
        return JsonPath.read(mockMvc.perform(get(url).with(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), path);
    }
}
