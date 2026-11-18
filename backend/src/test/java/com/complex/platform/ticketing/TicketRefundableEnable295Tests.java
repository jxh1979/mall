package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
 * 第十八批 AW 段 295-R1（BSM:785「「票种规则明确允许退款」这一项在后台出厂默认关闭；要让某一个票种可以退款，必须逐个票种单独开启，每次开启都须经高敏权限审批
 * 并留痕，不得整批打开」；DEC-R3B-2026-08-04-31；BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」）：建票不能直接可退；建票后逐个票种
 * 申请，持票种开启可退审批权限的直接上级批准后才可退，驳回不可退，申请人不能自批，待审期间不能重复申请；最高权限本人申请即刻可退。
 * 后台票务策略页与主管复核页真实请求（蛇形键）、真实授权链，随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TicketRefundableEnable295Tests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = String.valueOf(System.nanoTime());
    private final String project = "prj_c295_" + s;
    /** 票务运营（申请人）。 */
    private final String operator = "usr_c295_op_" + s;
    /** 申请人的直接上级：持票种开启可退审批权限。 */
    private final String approver = "usr_c295_ap_" + s;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '开启可退综合体', 'active', now(), now())", project);
        String operatorRole = role("票种开启可退运营" + s, List.of("ticket.product.manage", "ticket.product.view",
                "governance.supervisor_review.view", "governance.supervisor_review.approve"));
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", operator, operator);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", operator, operatorRole);
        String approverRole = role("票种开启可退审批" + s, List.of("ticket.product.refundable.approve",
                "governance.supervisor_review.view", "governance.supervisor_review.approve"));
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'staff', ?, NULL, 'active', now())", approver, approver, project);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", approver, approverRole);
        jdbc.update("INSERT INTO staff_level_assignments (user_id, staff_level_id, organization_id) VALUES (?, 'lvl_m3_upper', ?)",
                approver, project);
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id) VALUES (?, ?)", operator, approver);
    }

    @Test
    void createWithRefundableTrue_400() throws Exception {
        create("a", true).andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    @Test
    void requestStaysPendingUntilApproverApproves_selfApprovalAndDuplicateRequestRejected() throws Exception {
        String ticket = createdId("b");
        String review = JSON.readTree(request(ticket, operator(), "b").andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.review_status").value("pending_review"))
                        .andExpect(jsonPath("$.data.refundable").value(false))
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("review_id").asText();
        assertThat(refundable(ticket)).as("待审期间不可退（客人按「票种规则明确允许」申请退款仍被 TICKET_TYPE_NOT_REFUNDABLE 拒）").isFalse();

        request(ticket, operator(), "b2").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_REFUNDABLE_REQUEST_PENDING"));
        approve(operator(), review, "approved").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        assertThat(refundable(ticket)).isFalse();

        approve(as(approver, "staff", project), review, "approved").andExpect(status().isOk());
        assertThat(refundable(ticket)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'ticket_type.refundable_enabled'"
                + " AND business_id = ? AND metadata->>'review_id' = ?", Integer.class, ticket, review)).isEqualTo(1);
    }

    @Test
    void rejectedRequestLeavesTicketNotRefundable() throws Exception {
        String ticket = createdId("c");
        String review = JSON.readTree(request(ticket, operator(), "c").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data").path("review_id").asText();
        approve(as(approver, "staff", project), review, "rejected").andExpect(status().isOk());
        assertThat(refundable(ticket)).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'ticket_type.refundable_enable.rejected'"
                + " AND business_id = ?", Integer.class, ticket)).isEqualTo(1);
    }

    @Test
    void masterRequestTakesEffectAtOnce() throws Exception {
        String ticket = createdId("d");
        mockMvc.perform(post("/api/v1/ticketing/ticket-types/" + ticket + "/refundable-enable-requests").with(master())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c295-m-%s", "reason": "本人核对后开启可退"}
                                """.formatted(s)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("approved"))
                .andExpect(jsonPath("$.data.refundable").value(true));
        assertThat(refundable(ticket)).isTrue();
    }

    private ResultActions create(String tag, Boolean refundableFlag) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/ticket-types").with(operator())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "c295-%s-%s", "ticket_name": "开启可退票%s",
                         "base_calendar_price_mode": "weekday_weekend_holiday", "weekday_price_cent": 9900,
                         "weekend_price_cent": 9900, "holiday_price_cent": 9900, "stock_required": false, "refundable": %s}
                        """.formatted(tag, s, s, refundableFlag)));
    }

    private String createdId(String tag) throws Exception {
        String body = create(tag, null).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JSON.readTree(body).path("data").path("id").asText();
    }

    private ResultActions request(String ticket, RequestPostProcessor who, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/ticket-types/" + ticket + "/refundable-enable-requests").with(who)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "c295-r-%s-%s", "reason": "该票种按合同约定可退", "final_approver_id": "%s"}
                        """.formatted(tag, s, approver)));
    }

    private ResultActions approve(RequestPostProcessor who, String review, String decision) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + review + "/approve").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"" + decision + "\",\"comment\":\"已核对\",\"idempotency_key\":\"c295-ap-" + System.nanoTime() + "\"}"));
    }

    private boolean refundable(String ticket) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT refundable FROM ticket_types WHERE id = ?", Boolean.class, ticket));
    }

    private String role(String name, List<String> permissions) {
        String roleId = "role_c295_" + System.nanoTime();
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, view_all_projects, created_at)"
                + " VALUES (?, ?, '票种开启可退回归', 'platform', 'active', TRUE, now())", roleId, name);
        for (String code : permissions) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        return roleId;
    }

    private RequestPostProcessor operator() {
        return as(operator, "platform_admin", "org_1");
    }

    // 后台 Web 终端 + 生产过滤器放入的 AccessActor 主体（与账号表当前行一致）。
    private static RequestPostProcessor as(String userId, String accountType, String organization) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(
                    new AccessActor(userId, userId, accountType, organization, null, "active"), null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private static RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
