package com.complex.platform.governance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
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
 * 第 123 条 123-R1（BSM:1630「授权范围按项目在后台配置（业务参数，按项目可配，须同时配置上下限）。」）：各项目敏感操作授权范围
 * （圆心、授权半径、半径上下限）真库＋真实接口＋真实角色授权链回归，请求体按前端蛇形键。
 *
 * <p>夹具随测试事务回滚：项目 A、B 两行，操作人是归属 A 的员工，其角色持 platform.sensitive_operation_range.manage、不勾别的项目。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SensitiveOperationRangeJdbcIntegrationTests {

    @Autowired private AccessRepository accessRepository;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_r123_a_" + suffix;
    private final String projectB = "prj_r123_b_" + suffix;
    private RequestPostProcessor staffOfA;

    @BeforeEach
    void seed() {
        for (String projectId : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', '授权范围综合体', 'active', now(), now())", projectId);
        }
        String roleId = "role_r123_" + suffix;
        accessRepository.createRole(new RoleRecord(roleId, "敏感操作授权范围回归", null, "project", "active", Instant.now(),
                List.of("platform.sensitive_operation_range.manage"), null, false, List.of()));
        String userId = "usr_r123_" + suffix;
        accessRepository.createUser(new AccessUser(userId, userId, "staff", projectA, null, "active", Instant.now()));
        accessRepository.assignUserRole(userId, roleId);
        AccessActor actor = new AccessActor(userId, userId, "staff", projectA, null, "active");
        staffOfA = request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    // ① 未配置读回空；圆心、半径与上下限同填保存后读回一致，成功留痕 platform.sensitive_operation_range.saved。
    @Test
    void savedRangeReadsBack() throws Exception {
        String url = url(projectA);
        mockMvc.perform(get(url).with(staffOfA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project_id").value(projectA))
                .andExpect(jsonPath("$.data.radius_meters").doesNotExist());
        save(url, body("s1", "31.2304", "121.4737", "500", "100", "2000")).andExpect(status().isOk());
        mockMvc.perform(get(url).with(staffOfA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.center_latitude").value(31.2304))
                .andExpect(jsonPath("$.data.center_longitude").value(121.4737))
                .andExpect(jsonPath("$.data.radius_meters").value(500))
                .andExpect(jsonPath("$.data.min_radius_meters").value(100))
                .andExpect(jsonPath("$.data.max_radius_meters").value(2000));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'platform.sensitive_operation_range.saved'"
                + " AND event_result = 'success' AND business_id = ?", Integer.class, projectA)).isEqualTo(1);
    }

    // ② 缺半径下限、缺半径上限、半径小于下限、半径大于上限、纬度 91 一律 400 VALIDATION_FAILED，不落库。
    @Test
    void incompleteOrOutOfBoundsRangeRejected() throws Exception {
        String url = url(projectA);
        for (String body : List.of(
                body("v1", "31.2304", "121.4737", "500", null, "2000"),
                body("v2", "31.2304", "121.4737", "500", "100", null),
                body("v3", "31.2304", "121.4737", "50", "100", "2000"),
                body("v4", "31.2304", "121.4737", "3000", "100", "2000"),
                body("v5", "91", "121.4737", "500", "100", "2000"))) {
            save(url, body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        }
        assertThat(rows(projectA)).isZero();
    }

    // ③ 项目级账号改别的项目 403 CROSS_PROJECT_FORBIDDEN、不落库，失败留痕 platform.sensitive_operation_range.save_failed。
    @Test
    void projectStaffCannotConfigureOtherProject() throws Exception {
        save(url(projectB), body("x1", "31.2304", "121.4737", "500", "100", "2000"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(rows(projectB)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'platform.sensitive_operation_range.save_failed'"
                + " AND event_result = 'failure' AND business_id = ?", Integer.class, projectB)).isEqualTo(1);
    }

    private String url(String projectId) {
        return "/api/v1/platform/projects/" + projectId + "/sensitive-operation-range";
    }

    /** 值为 null 的字段不出现在请求体里（模拟漏填）。 */
    private String body(String key, String lat, String lng, String radius, String min, String max) {
        StringBuilder json = new StringBuilder("{\"idempotency_key\":\"" + key + "-" + suffix + "\"");
        String[][] fields = {{"center_latitude", lat}, {"center_longitude", lng}, {"radius_meters", radius},
                {"min_radius_meters", min}, {"max_radius_meters", max}};
        for (String[] field : fields) {
            if (field[1] != null) {
                json.append(",\"").append(field[0]).append("\":").append(field[1]);
            }
        }
        return json.append('}').toString();
    }

    private ResultActions save(String url, String body) throws Exception {
        return mockMvc.perform(put(url).with(staffOfA).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private Integer rows(String projectId) {
        return jdbc.queryForObject("SELECT count(*) FROM project_sensitive_operation_ranges WHERE project_id = ?", Integer.class, projectId);
    }
}
