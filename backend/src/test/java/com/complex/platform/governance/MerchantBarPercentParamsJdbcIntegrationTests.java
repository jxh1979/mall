package com.complex.platform.governance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import java.util.List;
import java.util.Map;
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
 * 第十八批 C 段两项按酒吧可配的百分比参数（值与上下限同配，范式同 TK-27 待清台提醒时限）：
 * 第 15 条 BSM:1468「经手员工的爽约率超过后台配置的上限时（业务参数，按酒吧可配，须同时配置上下限），自动停用其代订权限」——未配置不自动停用；
 * 第 17 条 BSM:2015「损耗率……超过后台配置的阈值……（安全阈值，按酒吧可配，出厂默认 5%，须同时配置上下限）」、第十八批裁决 4「这个阈值一并做出来」
 * ——未配置按出厂 5%，下限不得为 0（标准损耗＝标准出酒量×该阈值，BSM:1065「不得按零损耗记账」）。请求体按前端真实形状（蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MerchantBarPercentParamsJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "bpp_prj_" + suffix;
    private final String bar = "bpp_bar_" + suffix;
    private RequestPostProcessor manager;

    @BeforeEach
    void seed() {
        String company = "bpp_be_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','参数综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at) VALUES(?,'参数酒吧','bar',?,?,?,?,'active',now(),now())",
                bar, company, company, company, project);
        String user = "bpp_mgr_" + suffix;
        String role = "role_" + user;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'酒吧参数回归','project','active',now())", role, role);
        for (String permission : List.of("merchant.profile.view", "merchant.profile.manage")) {
            jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)", role, permission);
        }
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,NULL,'active',now())",
                user, user, project);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        AccessActor actor = new AccessActor(user, user, "staff", project, null, "active");
        manager = request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    // 爽约率上限：未配置三值皆空、不自动停用（无生效值）；值与上下限同填保存后回读；缺下限或值越界 400。
    @Test
    void staffBookingNoShowLimit_unconfiguredEmpty_configuredReadBack_partialOrOutOfBoundsRejected() throws Exception {
        String url = "/api/v1/platform/merchants/" + bar + "/staff-booking-no-show-limit";
        mockMvc.perform(get(url).with(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value_pct").doesNotExist())
                .andExpect(jsonPath("$.data.effective_pct").doesNotExist());
        configure(url, "{\"idempotency_key\":\"nsl-" + suffix + "\",\"value_pct\":30,\"min_pct\":10,\"max_pct\":50}")
                .andExpect(status().isOk());
        mockMvc.perform(get(url).with(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value_pct").value(30))
                .andExpect(jsonPath("$.data.min_pct").value(10))
                .andExpect(jsonPath("$.data.max_pct").value(50))
                .andExpect(jsonPath("$.data.effective_pct").value(30));
        configure(url, "{\"idempotency_key\":\"nsl-a-" + suffix + "\",\"value_pct\":30,\"max_pct\":50}")
                .andExpect(status().isBadRequest());
        configure(url, "{\"idempotency_key\":\"nsl-b-" + suffix + "\",\"value_pct\":60,\"min_pct\":10,\"max_pct\":50}")
                .andExpect(status().isBadRequest());
        Map<String, Object> row = jdbc.queryForMap("SELECT staff_booking_no_show_limit_pct AS v, staff_booking_no_show_limit_min_pct AS lo,"
                + " staff_booking_no_show_limit_max_pct AS hi FROM merchants WHERE id = ?", bar);
        assertThat(row).containsEntry("v", 30).containsEntry("lo", 10).containsEntry("hi", 50);
    }

    // 损耗率阈值：未配置按出厂 5%；值与上下限同填保存后生效值即所配值；缺上限、值越界、下限为 0 均 400。
    @Test
    void pourLossRate_unconfiguredFactory5_configuredReadBack_partialOutOfBoundsOrZeroMinRejected() throws Exception {
        String url = "/api/v1/platform/merchants/" + bar + "/pour-loss-rate";
        mockMvc.perform(get(url).with(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effective_pct").value(5))
                .andExpect(jsonPath("$.data.value_pct").doesNotExist());
        configure(url, "{\"idempotency_key\":\"plr-" + suffix + "\",\"value_pct\":8,\"min_pct\":3,\"max_pct\":10}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value_pct").value(8))
                .andExpect(jsonPath("$.data.effective_pct").value(8));
        configure(url, "{\"idempotency_key\":\"plr-a-" + suffix + "\",\"value_pct\":8,\"min_pct\":3}")
                .andExpect(status().isBadRequest());
        configure(url, "{\"idempotency_key\":\"plr-b-" + suffix + "\",\"value_pct\":12,\"min_pct\":3,\"max_pct\":10}")
                .andExpect(status().isBadRequest());
        configure(url, "{\"idempotency_key\":\"plr-c-" + suffix + "\",\"value_pct\":2,\"min_pct\":0,\"max_pct\":10}")
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT pour_loss_rate_pct FROM merchants WHERE id = ?", Integer.class, bar)).isEqualTo(8);
    }

    // 两个读口失败按 OpenAPI 声明留 merchant.view_failed（x-audit-event-failure）：商户不存在 404。
    @Test
    void percentParamReadFailureRecordsViewFailedAudit() throws Exception {
        String missing = "bpp_missing_" + suffix;
        for (String path : List.of("staff-booking-no-show-limit", "pour-loss-rate")) {
            mockMvc.perform(get("/api/v1/platform/merchants/" + missing + "/" + path).with(manager))
                    .andExpect(status().isNotFound());
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'merchant.view_failed'"
                + " AND event_result = 'failure' AND business_id = ?", Integer.class, missing)).isEqualTo(2);
    }

    private ResultActions configure(String url, String body) throws Exception {
        return mockMvc.perform(put(url).with(manager).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
