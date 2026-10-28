package com.complex.platform.inventory;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批第 24 条 24-R5：手工标记高价值。ENUM:164「或由库房主管以上岗位手工标记为高价值，标记与取消标记都必须填写理由并留痕，
 * 一线库管无此权限」；BSM:1196「或被手工标记为高价值（限库房主管及以上岗位）」。真库、真接口（请求体按前端蛇形键）：
 * 建物料不再能顺手标高价值；标记与取消走单独入口，持库房主管复核码（inventory.high_value.review）、理由必填、留前后值与理由。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class InventoryItemHighValueMarkJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private HighValueDecisionService decisions;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "g24m_prj_" + suffix;
    private final String merchant = "g24m_mch_" + suffix;
    private final String item = "g24m_item_" + suffix;

    @BeforeEach
    void seed() {
        String entity = "g24m_ent_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','高价值标记回归项目','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'门店主体','project_company','active',now(),now())", entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, project);
        for (String id : List.of(merchant, merchant + "_b")) {
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at) VALUES(?,?,'retail',?,?,'active',now(),now())", id, id, entity, project);
            jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,'effective','[\"inventory\",\"finance\"]'::jsonb,now(),now())", "ct_" + id, id, entity, entity);
        }
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at) VALUES(?,'标记测试酒水','liquor','瓶',0,'active',?,now(),now())", item, merchant);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 一线库管（只持 inventory.stock.move）建物料时带 manual_high_value: true 也不成为手工标记高价值。 */
    @Test
    void stockMoverCannotMarkHighValueWhenCreatingItem() throws Exception {
        RequestPostProcessor keeper = account("kpr", merchant, "'inventory.stock.move'");
        String created = json.readTree(mockMvc.perform(post("/api/v1/inventory/items").with(keeper).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"item_name\":\"新到酒水\",\"category\":\"liquor\","
                                + "\"unit\":\"瓶\",\"min_quantity\":0,\"manual_high_value\":true}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("id").asText();

        assertThat(jdbc.queryForObject("SELECT manual_high_value FROM inventory_items WHERE id=?", Boolean.class, created)).isFalse();
        assertThat(basis(created)).isNotEqualTo(HighValueDecisionService.BASIS_MANUAL_MARK);
    }

    /** 没有库房主管复核码不能标记（403）；别的商户的库房主管也不能标记本商户物料（403）。 */
    @Test
    void markingRequiresSupervisorPermissionAndOwnMerchant() throws Exception {
        mark(account("kpr", merchant, "'inventory.stock.move'"), true, "进口名酒").andExpect(status().isForbidden());
        mark(account("sup_b", merchant + "_b", "'inventory.high_value.review'"), true, "进口名酒")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT manual_high_value FROM inventory_items WHERE id=?", Boolean.class, item)).isFalse();
    }

    /** 库房主管标记与取消都必须填理由；标记后判定按手工标记，取消后回到按阈值；两次都留前后值与理由。 */
    @Test
    void supervisorMarksAndUnmarksWithReasonAndTrail() throws Exception {
        RequestPostProcessor supervisor = account("sup", merchant, "'inventory.high_value.review'");

        mark(supervisor, true, "").andExpect(status().isBadRequest());
        mark(supervisor, true, "进口名酒，到货价未录前先按高价值管")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.manual_high_value").value(true));
        assertThat(basis(item)).isEqualTo(HighValueDecisionService.BASIS_MANUAL_MARK);
        mark(supervisor, true, "重复标记").andExpect(status().isConflict());

        mark(supervisor, false, "已录到货价，按阈值判定")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.manual_high_value").value(false));
        assertThat(basis(item)).isNotEqualTo(HighValueDecisionService.BASIS_MANUAL_MARK);

        List<java.util.Map<String, Object>> trail = jdbc.queryForList("""
                SELECT metadata->>'reason' AS reason, metadata->>'manual_high_value_before' AS before_value,
                       metadata->>'manual_high_value_after' AS after_value
                  FROM audit_events WHERE event_name='inventory.item.high_value_marked' AND business_id=? ORDER BY occurred_at
                """, item);
        assertThat(trail).hasSize(2);
        assertThat(trail.get(0)).containsEntry("reason", "进口名酒，到货价未录前先按高价值管")
                .containsEntry("before_value", "false").containsEntry("after_value", "true");
        assertThat(trail.get(1)).containsEntry("reason", "已录到货价，按阈值判定")
                .containsEntry("before_value", "true").containsEntry("after_value", "false");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='inventory.item.high_value_mark_failed' AND business_id=?",
                Integer.class, item)).isEqualTo(2);
    }

    /** 物料「标记高价值」「取消标记」的真实请求形状（蛇形键）。 */
    private ResultActions mark(RequestPostProcessor who, boolean value, String reason) throws Exception {
        return mockMvc.perform(post("/api/v1/inventory/items/" + item + "/high-value-mark").with(who).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"manual_high_value\":" + value + ",\"reason\":\"" + reason + "\"}"));
    }

    private String basis(String itemId) {
        var previous = SecurityContextHolder.getContext().getAuthentication();
        try {
            TestSecurityActors.asPlatformAdmin();
            return decisions.decide(itemId, merchant).basis();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }

    private RequestPostProcessor account(String tag, String merchantId, String permissions) {
        String userId = "g24m_" + tag + "_" + suffix;
        String role = "g24m_role_" + tag + "_" + suffix;
        if (jdbc.queryForObject("SELECT count(*) FROM access_users WHERE id=?", Integer.class, userId) == 0) {
            jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'高价值标记回归','project','active',now())", role, role);
            jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN(" + permissions + ")", role);
            jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())", userId, userId, project, merchantId);
            jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
            jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        }
        AccessActor actor = new AccessActor(userId, userId, "staff", project, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
