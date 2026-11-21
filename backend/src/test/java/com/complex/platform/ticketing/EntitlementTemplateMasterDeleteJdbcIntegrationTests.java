package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
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

/**
 * 第十八批 BY 段第 383 条 383-R2（真库、真实入口、真实授权链）：通票权益模板删除只认最高权限本人（BSM:577「删除一律须由具备最高权限的人
 * 审批通过后执行，不设例外」）；最高权限本人发起即删（「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」），删前占用检查照走，
 * 已绑票种时 409 整笔回滚。票务写接口请求体带 idempotency_key。不开测试事务，才看得到回滚；角色名中文、编号后缀纯数字。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class EntitlementTemplateMasterDeleteJdbcIntegrationTests {

    private static final String BASE = "/api/v1/ticketing/entitlement-templates/";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private final String project = "r383tpl_prj_" + s;
    private final String requester = "usr_r383tpl_req_" + s;
    private final AtomicInteger keys = new AtomicInteger();

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','权益模板删除回归综合体','active',now(),now())", project);
    }

    /** 另一员工已申请删除的已存档模板：持 ticket.entitlement.approve 的普通员工审批 403 MASTER_APPROVAL_REQUIRED，仍待审批。 */
    @Test
    void nonMasterApproveDelete_403MasterApprovalRequired() throws Exception {
        String template = template("pending", true);
        RequestPostProcessor approver = new StaffRequestFixture(jdbc).staff("usr_r383tpl_apv_" + s, "staff", project, null,
                "ticket.entitlement.approve");

        send(approver, template, "delete-approve", Map.of("decision", "approved"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MASTER_APPROVAL_REQUIRED"));
        assertThat(deleteStatus(template)).isEqualTo("pending");
    }

    /** 最高权限本人对没绑票种的已存档模板申请删除：当场删除，含已存档列表里不再有它。 */
    @Test
    void masterDeleteRequestOnArchivedUnboundTemplate_takesEffect() throws Exception {
        String template = template("direct", false);

        send(master(), template, "delete-request", Map.of("reason", "停用的合作权益"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.delete_status").value("approved"));

        String body = mockMvc.perform(get("/api/v1/ticketing/entitlement-templates?includeArchived=true").with(master()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<List<String>>read(body, "$.data.items[*].id")).doesNotContain(template);
        assertThat(deleteStatus(template)).isEqualTo("approved");
    }

    /** 最高权限本人对已绑票种的已存档模板申请删除：409 ENTITLEMENT_TEMPLATE_IN_USE，整笔回滚，不留待审批。 */
    @Test
    void masterDeleteRequestOnTemplateBoundToTicketType_409EntitlementTemplateInUse_rolledBack() throws Exception {
        String template = template("bound", false);
        String ticketType = "tt_r383tpl_" + s;
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at)"
                + " VALUES (?, '权益模板删除回归票种', 'fixed', 'active', now(), now())", ticketType);
        jdbc.update("INSERT INTO ticket_type_entitlements (id, ticket_type_id, entitlement_template_id, quantity, total_times,"
                + " num_people, value_amount_cent, component_price_cent, created_at, updated_at)"
                + " VALUES (?, ?, ?, 1, 1, 1, 100, 0, now(), now())", "bind_r383tpl_" + s, ticketType, template);

        send(master(), template, "delete-request", Map.of("reason", "停用的合作权益"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ENTITLEMENT_TEMPLATE_IN_USE"));
        assertThat(deleteStatus(template)).isEqualTo("none");
    }

    // ---------- 夹具 ----------

    /** 已存档的权益模板；pending=true 时另一位员工已申请删除、待审批。 */
    private String template(String tag, boolean pending) {
        String id = "tpl_r383_" + tag + "_" + s;
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,"
                + " archived_at, archived_by, archive_reason, delete_status, delete_requested_by, delete_requested_at, delete_reason,"
                + " created_at, updated_at)"
                + " VALUES (?, ?, 'ticket', 1, 100, 'active', now(), ?, '停用', CASE WHEN ? THEN 'pending' ELSE 'none' END,"
                + " CASE WHEN ? THEN ? END, CASE WHEN ? THEN now() END, CASE WHEN ? THEN '停用的合作权益' END, now(), now())",
                id, "回归权益" + tag, requester, pending, pending, requester, pending, pending);
        return id;
    }

    // ---------- 请求与断言 ----------

    private ResultActions send(RequestPostProcessor who, String template, String action, Map<String, String> fields) throws Exception {
        Map<String, String> body = new java.util.HashMap<>(fields);
        body.put("idempotency_key", "r383tpl-" + action + "-" + keys.incrementAndGet() + "-" + s);
        return mockMvc.perform(post(BASE + template + "/" + action).with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private String deleteStatus(String template) {
        return jdbc.queryForObject("SELECT delete_status FROM entitlement_templates WHERE id = ?", String.class, template);
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
