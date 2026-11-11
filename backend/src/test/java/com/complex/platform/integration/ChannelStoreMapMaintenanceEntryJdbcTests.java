package com.complex.platform.integration;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.Matchers;
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
 * 第十八批 AE 段第 201 条 201-R6（决策台账 171-R1 先例「契约与代码一致」）：渠道门店映射三个入口补进 OpenAPI 后，
 * 按契约声明的请求体（蛇形键、只发声明字段）走真实入口与真实授权链：登记、列表、停用成功并落契约写的成功事件；
 * 停用缺理由 400 VALIDATION_FAILED、映射不存在 404 INTEGRATION_CHANNEL_STORE_MAP_NOT_FOUND。入口已存在，本类只锁现行为。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ChannelStoreMapMaintenanceEntryJdbcTests {

    private static final String MAPS = "/api/v1/integration/channel-store-maps";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "csme_prj_" + s;
    private final String merchant = "csme_mch_" + s;
    private RequestPostProcessor admin;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', '渠道映射回归综合体', 'active', now(), now())",
                project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) VALUES (?, '渠道映射回归餐厅', 'catering', 'active', now(), now(), ?)",
                merchant, project);
        admin = new StaffRequestFixture(jdbc).staff("usr_csme_" + s, "platform_admin", project, null,
                "integration.channel_store_map.view", "integration.channel_store_map.manage");
    }

    @Test
    void saveListArchiveWithContractBodies() throws Exception {
        String id = json.readTree(send(MAPS, Map.of("idempotency_key", "csme-save-" + s, "provider_code", "csme-pos",
                        "channel", "meituan", "external_store_id", "mt-store-" + s, "merchant_id", merchant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("active"))
                .andExpect(jsonPath("$.data.merchant_id").value(merchant))
                .andReturn().getResponse().getContentAsString()).path("data").path("id").asText();

        mockMvc.perform(get(MAPS).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", Matchers.hasItem(id)));

        send(MAPS + "/" + id + "/archive", Map.of("idempotency_key", "csme-archive-" + s, "reason", "门店闭店，停用映射"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("inactive"));

        assertThat(events("integration.channel_store_map.saved", id)).isEqualTo(1);
        assertThat(events("integration.channel_store_map.archived", id)).isEqualTo(1);
    }

    @Test
    void archiveRejectsMissingReasonAndUnknownMap() throws Exception {
        jdbc.update("""
                INSERT INTO integration_channel_store_map (id, provider_code, channel, external_store_id, merchant_id, status, created_at, updated_at)
                VALUES (?, 'csme-pos', 'douyin', ?, ?, 'active', now(), now())""", "csme_map_" + s, "dy-store-" + s, merchant);

        send(MAPS + "/csme_map_" + s + "/archive", Map.of("idempotency_key", "csme-noreason-" + s))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT status FROM integration_channel_store_map WHERE id = ?", String.class, "csme_map_" + s))
                .isEqualTo("active");

        send(MAPS + "/csme_missing_" + s + "/archive", Map.of("idempotency_key", "csme-missing-" + s, "reason", "门店闭店，停用映射"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("INTEGRATION_CHANNEL_STORE_MAP_NOT_FOUND"));
    }

    private ResultActions send(String path, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post(path).with(admin).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private Integer events(String eventName, String businessId) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = ? AND business_id = ? AND event_result = 'success'",
                Integer.class, eventName, businessId);
    }
}
