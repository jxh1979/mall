package com.complex.platform.parking;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
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
 * HP-P01 R1 真库、真接口（请求体按前端蛇形键）：BSM L1434「免费时长、超出后按小时计费（不足一小时按一小时）、每日封顶三项均为业务参数
 * （按项目可配，须同时配置上下限；出厂默认值实施前补定）」；L1435 离场宽限、L1436 门槛与抵免时长、L1437 员工免费或折扣同为按项目、
 * 须同时配置上下限的业务参数。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ParkingPolicyJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private ParkingFixture f;
    private RequestPostProcessor manager;

    @BeforeEach
    void seed() {
        f = new ParkingFixture(jdbc, "pol");
        manager = f.account("mgr", f.role("mgr", "parking.config.manage", "parking.ops.view"), null);
    }

    @Test
    void saveRejectsMissingBounds() throws Exception {
        Map<String, Object> body = body();
        body.remove("daily_cap_cent_max");
        save(manager, f.project, body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("PARKING_POLICY_BOUNDS_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_policies WHERE project_id=?", Integer.class, f.project)).isZero();
    }

    @Test
    void saveRejectsValueOutsideBounds() throws Exception {
        Map<String, Object> body = body();
        body.put("free_minutes", 150);
        save(manager, f.project, body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("PARKING_POLICY_OUT_OF_BOUNDS"));
        Map<String, Object> discount = body();
        discount.put("staff_discount_bp_max", 12000);
        save(manager, f.project, discount).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("PARKING_POLICY_OUT_OF_BOUNDS"));
    }

    @Test
    void projectScopedAccountCannotSaveOtherProject() throws Exception {
        ParkingFixture other = new ParkingFixture(jdbc, "pol2");
        save(manager, other.project, body()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
    }

    @Test
    void unconfiguredProjectIsConflictNotDefault() throws Exception {
        mockMvc.perform(get("/api/v1/parking/policies/" + f.project).with(manager))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PARKING_POLICY_NOT_CONFIGURED"));
    }

    @Test
    void saveWithSnakeCaseBodyLikeAdminClient() throws Exception {
        save(manager, f.project, body()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.free_minutes").value(30))
                .andExpect(jsonPath("$.data.free_minutes_min").value(0))
                .andExpect(jsonPath("$.data.free_minutes_max").value(120))
                .andExpect(jsonPath("$.data.staff_discount_bp").value(5000));
        mockMvc.perform(get("/api/v1/parking/policies/" + f.project).with(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hourly_rate_cent").value(500))
                .andExpect(jsonPath("$.data.exit_grace_minutes_max").value(60));
        // 改值后再存：只改本项目这一行。
        Map<String, Object> changed = body();
        changed.put("hourly_rate_cent", 800);
        save(manager, f.project, changed).andExpect(status().isOk()).andExpect(jsonPath("$.data.hourly_rate_cent").value(800));
        assertThat(jdbc.queryForObject("SELECT hourly_rate_cent FROM parking_policies WHERE project_id=?", Long.class, f.project))
                .isEqualTo(800L);
    }

    private ResultActions save(RequestPostProcessor who, String project, Map<String, Object> body) throws Exception {
        body.putIfAbsent("idempotency_key", "pol_" + java.util.UUID.randomUUID());
        return mockMvc.perform(put("/api/v1/parking/policies/" + project).with(who).contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body)));
    }

    /** 前端 adminClient.mapKeys 后的形状：值与上下限成对、全部蛇形。 */
    private static Map<String, Object> body() {
        Map<String, Object> b = new LinkedHashMap<>();
        pair(b, "free_minutes", 30, 0, 120);
        pair(b, "hourly_rate_cent", 500, 0, 2000);
        pair(b, "daily_cap_cent", 3000, 0, 10000);
        pair(b, "exit_grace_minutes", 15, 0, 60);
        pair(b, "credit_threshold_cent", 20000, 0, 100000);
        pair(b, "credit_minutes", 120, 0, 480);
        pair(b, "staff_discount_bp", 5000, 0, 10000);
        return b;
    }

    private static void pair(Map<String, Object> b, String key, int value, int min, int max) {
        b.put(key, value);
        b.put(key + "_min", min);
        b.put(key + "_max", max);
    }
}
