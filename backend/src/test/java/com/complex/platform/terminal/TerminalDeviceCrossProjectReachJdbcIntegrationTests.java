package com.complex.platform.terminal;

// 文件版本：V0.02

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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 G 段第 82 条 82b-R5 ④（终端设备的列表与各写入口按入口权限码可触及的项目判）与 82-R18b（第十八批裁决 18「设备绑了空间的按空间所在项目，
 * 没绑的仍按主项目」）真库＋真实接口＋真实角色勾选回归，请求体一律按前端蛇形键。
 * 依据：治理地图第 659 行「被分配了可跨项目角色的员工（含经岗位得到），在所分配的权限内可以查看、处理该角色可查看项目的数据。」
 * 「逐个勾选的只能查看被选中的项目」；第 528 行「不受第④类「别的项目」限制」；裁决 18 所选选项原文。
 *
 * <p>夹具随测试事务回滚：项目 A、B（编号带随机后缀）；商户 m 主项目 A；空间 A、B；持有员工档案 A、B。共享库里有别的设备：列表断言只看
 * 本次夹具建的行（编号都以随机后缀结尾）。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TerminalDeviceCrossProjectReachJdbcIntegrationTests {

    private static final String DEVICES = "/api/v1/terminals/devices";
    private static final String[] CODES = {"terminal.device.bind", "terminal.device.lifecycle", "terminal.switch.view"};

    @Autowired private AccessRepository accessRepository;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_r5d_a_" + suffix;
    private final String projectB = "prj_r5d_b_" + suffix;
    private final String merchant = "mch_r5d_m_" + suffix;
    private final String spaceA = "spc_r5d_a_" + suffix;
    private final String spaceB = "spc_r5d_b_" + suffix;
    private final String holderA = "stf_r5d_a_" + suffix;
    private final String holderB = "stf_r5d_b_" + suffix;

    @BeforeEach
    void seed() {
        for (String project : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', ?, 'active', now(), now())", project, "R5D " + project);
        }
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                + "VALUES (?, ?, 'bar', 'active', now(), now(), ?)", merchant, "R5D " + merchant, projectA);
        space(spaceA, projectA);
        space(spaceB, projectB);
        holder(holderA, projectA);
        holder(holderB, projectB);
    }

    // 82b-R5 ④：设备列表按 terminal.switch.view 可触及的项目——A 普通员工只出 A 的设备；只勾 B 的持有人只出 B 的设备，不出本人项目 A
    // （该码没有别的来源）。修复前持有人按本人项目 A 取（第一处断言即红）。
    @Test
    void theDeviceListShowsDevicesOfTheProjectsTheEntryCodeReaches() throws Exception {
        String inA = device("la", "bound", null, spaceA, null);
        String inB = device("lb", "bound", null, spaceB, null);
        RequestPostProcessor ticked = staff("lt", projectA, role("lt", List.of(projectB)));
        RequestPostProcessor plain = staff("lp", projectA, role("lp", List.of()));

        assertThat(listed(ticked)).containsExactly(inB);
        assertThat(listed(plain)).containsExactly(inA);
    }

    // 82b-R5 ④：既有设备的写按设备认定项目与入口码判——只勾 B 的持有人把 B 的设备领用到 B 的空间 200、送修时改由 B 的员工持有 200；
    // 改由 A 的员工持有 403（新持有员工须在可触及范围内，本人项目 A 不在）；A 普通员工领用同一台 403。修复前持有人领用 403（断言即红）。
    @Test
    void writesOnDevicesFollowTheDeviceProjectAndTheEntryCode() throws Exception {
        String inB = device("wb", "returned", null, spaceB, null);
        RequestPostProcessor ticked = staff("wt", projectA, role("wt", List.of(projectB)));
        RequestPostProcessor plain = staff("wp", projectA, role("wp", List.of()));

        send(plain, DEVICES + "/bind", bindJson("wp", "wb", null, spaceB)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        send(ticked, DEVICES + "/bind", bindJson("wt", "wb", null, spaceB)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("bound"));

        send(ticked, DEVICES + "/" + inB + "/repair", holderJson("wt-a", holderA)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        send(ticked, DEVICES + "/" + inB + "/repair", holderJson("wt-b", holderB)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("repair"));
        assertThat(jdbc.queryForObject("SELECT staff_id FROM terminal_devices WHERE id = ?", String.class, inB)).isEqualTo(holderB);
    }

    // 82-R18b：挂商户 m（主项目 A）的设备绑了 B 的空间按 B 算——B 普通员工能列出、送修，A 普通员工 403；B 普通员工给 m 在 B 的空间登记新设备 200。
    // 没绑空间的仍按 m 的主项目 A：B 普通员工 403，A 普通员工 200。修复前按商户主项目 A 算（A 普通员工送修 B 空间设备 200，第一处断言即红）。
    @Test
    void aMerchantDeviceBoundToASpaceBelongsToTheSpaceProject() throws Exception {
        String atB = device("mb", "bound", merchant, spaceB, null);
        String noSpace = device("mn", "bound", merchant, null, null);
        RequestPostProcessor plainA = staff("ma", projectA, role("ma", List.of()));
        RequestPostProcessor plainB = staff("mbb", projectB, role("mbb", List.of()));

        send(plainA, DEVICES + "/" + atB + "/repair", reasonJson("ma-b")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(listed(plainB)).contains(atB).doesNotContain(noSpace);
        assertThat(listed(plainA)).contains(noSpace).doesNotContain(atB);
        send(plainB, DEVICES + "/" + atB + "/repair", reasonJson("mb-b")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("repair"));
        send(plainB, DEVICES + "/bind", bindJson("mb-new", "mnew", merchant, spaceB)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchant_id").value(merchant));

        send(plainB, DEVICES + "/" + noSpace + "/repair", reasonJson("mb-n")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        send(plainA, DEVICES + "/" + noSpace + "/repair", reasonJson("ma-n")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("repair"));
    }

    // 第十八批 AG 段 259-R6（OpenAPI 与代码一致：markTerminalDeviceRepair 等生命周期入口的 reason 进 required）：送修不带 reason → 400
    // 「原因未填写」、设备仍为 bound；带上原因 → 200、设备变为 repair。
    @Test
    void repairWithoutReasonIsRejected() throws Exception {
        String inA = device("nr", "bound", null, spaceA, null);
        RequestPostProcessor plain = staff("nr", projectA, role("nr", List.of()));

        send(plain, DEVICES + "/" + inA + "/repair", "{\"idempotency_key\": \"r5d-nr-none-" + suffix + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("原因未填写"));
        assertThat(jdbc.queryForObject("SELECT status FROM terminal_devices WHERE id = ?", String.class, inA)).isEqualTo("bound");
        send(plain, DEVICES + "/" + inA + "/repair", reasonJson("nr-ok")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("repair"));
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private void space(String id, String project) {
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, 'shop', 'active', now(), now())", id, project, "R5D " + id);
    }

    private void holder(String id, String project) {
        jdbc.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id,
                        registration_status, status, created_at, updated_at)
                VALUES (?, ?, NULL, ?, 'scoped', ?, 'approved', 'active', now(), now())
                """, id, project, "usr_" + id, project);
    }

    // 一台平板；所属商户、绑定空间、持有员工都可为空。返回记录 id（接口路径里的 id）。
    private String device(String tag, String status, String merchantId, String space, String staff) {
        String id = "td_r5d_" + tag + "_" + suffix;
        jdbc.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope,
                    bound_space_id, merchant_id, staff_id, status, created_at, updated_at)
                VALUES (?, ?, ?, 'tablet', 'space', ?, ?, ?, ?, now(), now())
                """, id, deviceId(tag), "DC-r5d-" + tag + "-" + suffix, space, merchantId, staff, status);
        return id;
    }

    private String deviceId(String tag) {
        return "dev-r5d-" + tag + "-" + suffix;
    }

    private String bindJson(String key, String tag, String merchantId, String space) {
        return """
                {"idempotency_key": "r5d-%s-%s", "device_id": "%s", "terminal_type": "tablet", "binding_scope": "space",
                 "bound_space_id": "%s", "merchant_id": %s}
                """.formatted(key, suffix, deviceId(tag), space, merchantId == null ? "null" : "\"" + merchantId + "\"");
    }

    private String reasonJson(String key) {
        return "{\"idempotency_key\": \"r5d-" + key + "-" + suffix + "\", \"reason\": \"屏幕碎裂\"}";
    }

    private String holderJson(String key, String staff) {
        return "{\"idempotency_key\": \"r5d-" + key + "-" + suffix + "\", \"reason\": \"屏幕碎裂\", \"staff_id\": \"" + staff + "\"}";
    }

    // ── 角色与会话（照 CrossProjectGovernanceJdbcIntegrationTests） ─────────────────────────────────

    private String role(String tag, List<String> ticked) {
        String roleId = "role_r5d_" + tag + "_" + suffix;
        accessRepository.createRole(new RoleRecord(roleId, "R5D " + tag + " " + suffix, null, "project", "active", Instant.now(),
                List.of(CODES), null, false, ticked));
        return roleId;
    }

    /** 建一名员工账号、挂上给定角色，返回以其身份登录的会话（后台 Web 终端＋生产过滤器放入的 AccessActor 主体，与账号行一致）。 */
    private RequestPostProcessor staff(String tag, String organizationId, String roleId) {
        String userId = "usr_r5d_" + tag + "_" + suffix;
        accessRepository.createUser(new AccessUser(userId, userId, "staff", organizationId, null, "active", Instant.now()));
        accessRepository.assignUserRole(userId, roleId);
        AccessActor actor = new AccessActor(userId, userId, "staff", organizationId, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private ResultActions send(RequestPostProcessor session, String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(session).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** 设备列表里属于本次夹具的记录 id（都以随机后缀结尾）；共享库里别的设备不看。 */
    private List<String> listed(RequestPostProcessor session) throws Exception {
        List<String> ids = JsonPath.read(mockMvc.perform(get(DEVICES).with(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.items[*].id");
        return ids.stream().filter(id -> id != null && id.endsWith(suffix)).toList();
    }
}
