package com.complex.platform.evidence;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AN 段第 227 条 227-R2（PGM:287「……字段名、状态、枚举取值……必须全部为中文……不得以“这是标识”为由保留英文。」）：
 * 后台审计事件页操作日志视图按 GET /api/v1/audit/labels 显示业务类型、操作人类型、权限中文名。员工经真实入口办一笔上线准备项保存，
 * 平台管理员登录后取回这笔操作的留痕行，行里的业务类型、操作人类型、权限码在标签接口各节里都有中文名。
 * 227-R3：再办一笔缺责任人的保存（400，记失败留痕），取回的各行事件名在标签接口 event_names 里都有中文名。
 * 18AN 收尾复核修正：页面传来的配置组码作业务类型的转正留痕、最高权限会话办事的留痕，业务类型与操作人类型同样有中文名。
 * 18AN 合并前再变基（a890a1ac）：变基带入的 18AP 存酒组留痕，事件名与业务类型同样有中文名。
 */
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "complex.smoke.admin.username=p0_jdbc_test_admin",
        "complex.smoke.admin.password=p0_jdbc_test_password"
})
@Transactional
class AuditOperationLabelsJdbcIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.complex.platform.common.BusinessDayResolver businessDays;

    @Test
    void operationRowsOfARealUpdateHaveChineseLabels() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String user = "usr_audit_labels_" + suffix;
        RequestPostProcessor staff = staffWith(user, suffix, List.of("ops.production_readiness.view", "ops.production_readiness.manage"));
        String admin = "Bearer " + adminToken();
        JsonNode drill = null;
        for (JsonNode item : data(mockMvc.perform(get("/api/v1/ops/production-readiness/items").with(staff))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("items")) {
            if ("nas-restore-drill".equals(item.path("id").asText())) {
                drill = item;
            }
        }
        assertThat(drill).isNotNull();
        mockMvc.perform(put("/api/v1/ops/production-readiness/items/nas-restore-drill").with(staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "idempotency_key", "audit-labels-227-" + suffix,
                                "status", "passed",
                                "evidence_link", "evidence://nas/restore-drill/202611",
                                "owner_person", "运维值班负责人",
                                "owner_department", drill.path("owner_department").asText(),
                                "target_date", drill.path("target_date").asText(),
                                "blocking_scope", drill.path("blocking_scope").asText(),
                                "fallback_mode", drill.path("fallback_mode").asText()))))
                .andExpect(status().isOk());

        JsonNode rows = data(mockMvc.perform(get("/api/v1/audit/operation-events")
                        .param("business_type", "production_readiness_item").param("actor_id", user).header("Authorization", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        List<String> events = rows.findValuesAsText("event_name");
        assertThat(events).contains("production_readiness.updated", "production_readiness.viewed");

        String labelsBody = mockMvc.perform(get("/api/v1/audit/labels").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode labels = data(labelsBody);
        Map<String, String> businessTypes = section(labels, "business_types");
        Map<String, String> actorTypes = section(labels, "actor_types");
        Map<String, String> permissions = section(labels, "permissions");
        for (JsonNode row : rows) {
            assertThat(businessTypes.get(row.path("business_type").asText())).isNotBlank().doesNotContainPattern("[A-Za-z]");
            assertThat(actorTypes.get(row.path("actor_type").asText())).isNotBlank().doesNotContainPattern("[A-Za-z]");
            assertThat(permissions.get(row.path("permission_code").asText())).isNotBlank().doesNotContainPattern("[A-Za-z]");
        }
        // 权限表外的权限状态与伪码（登录、积分入账等每天都有的留痕）同样有中文名。
        assertThat(permissions.get("auth.login")).isNotBlank().doesNotContainPattern("[A-Za-z]");
        assertThat(permissions.get("not_applicable")).isNotBlank().doesNotContainPattern("[A-Za-z]");
        // 带下划线的码原样作 code 返回（不被客户端的键名转换动到）。
        assertThat(businessTypes).containsKey("production_readiness_item");
        assertThat(labels.path("event_names").isArray()).isTrue();
    }

    @Test
    void staffWithoutOperationLogViewCannotReadLabels() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        RequestPostProcessor staff = staffWith("usr_audit_labels_" + suffix, suffix, List.of("ops.production_readiness.view"));
        mockMvc.perform(get("/api/v1/audit/labels").with(staff)).andExpect(status().isForbidden());
    }

    /**
     * 227-R3（PGM:287「……展示给用户的一切文字……必须全部为中文，不得出现任何英文……」）：同一上线准备项经真实入口办一笔成功保存、
     * 一笔缺责任人的保存（400，失败留痕），平台管理员取回的各行事件名（查看、保存成功、保存失败）在 event_names 里都有中文名。
     */
    @Test
    void operationEventNamesOfARealUpdateHaveChineseLabels() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String user = "usr_audit_labels_" + suffix;
        RequestPostProcessor staff = staffWith(user, suffix, List.of("ops.production_readiness.view", "ops.production_readiness.manage"));
        String admin = "Bearer " + adminToken();
        JsonNode drill = null;
        for (JsonNode item : data(mockMvc.perform(get("/api/v1/ops/production-readiness/items").with(staff))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("items")) {
            if ("nas-restore-drill".equals(item.path("id").asText())) {
                drill = item;
            }
        }
        assertThat(drill).isNotNull();
        Map<String, String> body = new HashMap<>(Map.of(
                "idempotency_key", "audit-labels-227r3-" + suffix,
                "status", "passed",
                "evidence_link", "evidence://nas/restore-drill/202611",
                "owner_person", "运维值班负责人",
                "owner_department", drill.path("owner_department").asText(),
                "target_date", drill.path("target_date").asText(),
                "blocking_scope", drill.path("blocking_scope").asText(),
                "fallback_mode", drill.path("fallback_mode").asText()));
        mockMvc.perform(put("/api/v1/ops/production-readiness/items/nas-restore-drill").with(staff)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        body.remove("owner_person");
        body.put("idempotency_key", "audit-labels-227r3-missing-owner-" + suffix);
        mockMvc.perform(put("/api/v1/ops/production-readiness/items/nas-restore-drill").with(staff)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        JsonNode rows = data(mockMvc.perform(get("/api/v1/audit/operation-events")
                        .param("business_type", "production_readiness_item").param("actor_id", user).header("Authorization", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(rows.findValuesAsText("event_name"))
                .contains("production_readiness.viewed", "production_readiness.updated", "production_readiness.update_failed");

        Map<String, String> eventNames = section(data(mockMvc.perform(get("/api/v1/audit/labels").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)), "event_names");
        for (JsonNode row : rows) {
            assertThat(eventNames.get(row.path("event_name").asText()))
                    .as("事件名 %s 的中文名", row.path("event_name").asText()).isNotBlank().doesNotContainPattern("[A-Za-z]");
        }
    }

    /**
     * 18AN 收尾复核修正（PGM:287「……字段名、状态、枚举取值……必须全部为中文……」）：财务甲经真实入口落一条卡座提成费率草案，
     * 财务乙在待转正队列点转正（POST /api/v1/settlement/config/commission_rate/{id}/activate），这笔转正留痕的业务类型是页面传来的
     * 配置组码 commission_rate，在标签接口 business_types 里有中文名。
     */
    @Test
    void activatedConfigDraftRowHasChineseBusinessTypeLabel() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String drafter = "usr_audit_labels_d_" + suffix;
        String reviewer = "usr_audit_labels_r_" + suffix;
        RequestPostProcessor financeA = staffWith(drafter, suffix + "1", List.of("settlement.config.manage"));
        RequestPostProcessor financeB = staffWith(reviewer, suffix + "2", List.of("settlement.config.manage"));
        String admin = "Bearer " + adminToken();
        mockMvc.perform(post("/api/v1/settlement/config/commission-rate").with(financeA).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "idempotency_key", "audit-labels-cfg-" + suffix, "role_type", "sales", "rate_bp", 1200,
                                // 第十八批 BQ 段第 371 条 371-R1：生效起始日期必填（全平台默认行按平台默认营业日，次日）。
                                "effective_from", businessDays.businessDate(null, null, java.time.Instant.now()).plusDays(1).toString()))))
                .andExpect(status().isOk());
        String draftId = jdbcTemplate.queryForObject(
                "SELECT id FROM bar_commission_rate_configs WHERE is_draft = TRUE AND status = 'active' AND updated_by = ?", String.class, drafter);
        mockMvc.perform(post("/api/v1/settlement/config/commission_rate/" + draftId + "/activate").with(financeB))
                .andExpect(status().isOk());

        JsonNode rows = data(mockMvc.perform(get("/api/v1/audit/operation-events")
                        .param("business_type", "commission_rate").param("actor_id", reviewer).header("Authorization", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(rows.findValuesAsText("event_name")).contains("settlement.config.draft.activated");
        Map<String, String> businessTypes = section(data(mockMvc.perform(get("/api/v1/audit/labels").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)), "business_types");
        for (JsonNode row : rows) {
            assertThat(businessTypes.get(row.path("business_type").asText()))
                    .as("业务类型 %s 的中文名", row.path("business_type").asText()).isNotBlank().doesNotContainPattern("[A-Za-z]");
        }
    }

    /**
     * 18AN 收尾复核修正（PGM:287；PGM:658「……最高权限审批」）：最高权限会话经真实入口查看上线准备项，这笔留痕的操作人类型是
     * master_authority，在标签接口 actor_types 里有中文名「最高权限」。
     */
    @Test
    void masterAuthorityRowHasChineseActorTypeLabel() throws Exception {
        String admin = "Bearer " + adminToken();
        AccessActor masterActor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, null, null, "active");
        RequestPostProcessor master = request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(masterActor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
        mockMvc.perform(get("/api/v1/ops/production-readiness/items").with(master)).andExpect(status().isOk());

        JsonNode rows = data(mockMvc.perform(get("/api/v1/audit/operation-events")
                        .param("business_type", "production_readiness_item").param("actor_id", SessionIssuer.MASTER_USER_ID)
                        .header("Authorization", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(rows.findValuesAsText("actor_type")).contains(SessionIssuer.MASTER_ACCOUNT_TYPE);
        Map<String, String> actorTypes = section(data(mockMvc.perform(get("/api/v1/audit/labels").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)), "actor_types");
        assertThat(actorTypes.get(SessionIssuer.MASTER_ACCOUNT_TYPE)).isEqualTo("最高权限");
    }

    /**
     * 18AN 合并前再变基（a890a1ac，含 18AP 存酒组；PGM:287「……展示给用户的一切文字……必须全部为中文，不得出现任何英文……」）：
     * 员工在存酒页读存酒有效期、转存次数上限与顺延期限（GET /api/v1/inventory/saved-bottles/renewal-policy），再在存酒登记上传登记照片
     * （POST /api/v1/evidence/attachments/upload，business_type=saved_bottle_registration，空文件，上传失败记失败留痕）；这两笔留痕的事件名
     * 与业务类型在标签接口 event_names、business_types 里都有中文名。
     */
    @Test
    void savedBottleRowsFromRebasedMainHaveChineseLabels() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String user = "usr_audit_labels_w_" + suffix;
        RequestPostProcessor staff = staffWith(user, suffix, List.of("bar.saved_bottle.renewal_policy.manage", "evidence.attachment.create"));
        String admin = "Bearer " + adminToken();
        mockMvc.perform(get("/api/v1/inventory/saved-bottles/renewal-policy").with(staff)).andExpect(status().isOk());
        mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", "register.jpg", "image/jpeg", new byte[0]))
                        .param("business_type", "saved_bottle_registration").param("attachment_role", "photo").with(staff))
                .andExpect(status().is4xxClientError());

        JsonNode rows = data(mockMvc.perform(get("/api/v1/audit/operation-events").param("actor_id", user).header("Authorization", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(rows.findValuesAsText("event_name")).contains("bar.saved_bottle.renewal_policy_viewed", "evidence.attachment_upload_failed");
        assertThat(rows.findValuesAsText("business_type")).contains("saved_bottle_registration");
        JsonNode labels = data(mockMvc.perform(get("/api/v1/audit/labels").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        Map<String, String> eventNames = section(labels, "event_names");
        Map<String, String> businessTypes = section(labels, "business_types");
        for (JsonNode row : rows) {
            assertThat(eventNames.get(row.path("event_name").asText()))
                    .as("事件名 %s 的中文名", row.path("event_name").asText()).isNotBlank().doesNotContainPattern("[A-Za-z]");
            assertThat(businessTypes.get(row.path("business_type").asText()))
                    .as("业务类型 %s 的中文名", row.path("business_type").asText()).isNotBlank().doesNotContainPattern("[A-Za-z]");
        }
    }

    private String adminToken() throws Exception {
        return data(mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login_name\":\"p0_jdbc_test_admin\",\"password\":\"p0_jdbc_test_password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("access_token").asText();
    }

    private JsonNode data(String body) throws Exception {
        return objectMapper.readTree(body).path("data");
    }

    private static Map<String, String> section(JsonNode labels, String name) {
        Map<String, String> map = new HashMap<>();
        labels.path(name).forEach(label -> map.put(label.path("code").asText(), label.path("name").asText()));
        return map;
    }

    /** 平台公司员工经一个普通角色持有给定权限（后台 Web 终端）。 */
    private RequestPostProcessor staffWith(String user, String suffix, List<String> codes) {
        String role = "role_audit_labels_" + suffix;
        jdbcTemplate.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'第227条','project','active',now())",
                role, "审计标签" + suffix);
        for (String code : codes) {
            jdbcTemplate.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)", role, code);
        }
        jdbcTemplate.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff','org',NULL,'active',now())",
                user, user);
        jdbcTemplate.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        AccessActor actor = new AccessActor(user, user, "staff", "org", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
