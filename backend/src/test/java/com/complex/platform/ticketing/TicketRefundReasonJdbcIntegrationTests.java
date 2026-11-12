package com.complex.platform.ticketing;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketRefundRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * TK-05-R1 普通退款理由五类闭集（BSM:804「只有票种规则明确允许、活动或场次取消、平台或履约方无法提供服务、重复扣款、法律法规要求这五种情形
 * 才允许退款……确需在五种情形之外退款的，只能由持授权 U 盘密钥的最高权限逐笔放行」「最高权限逐笔放行不解除已开始核销后的退款禁止」；
 * BSM:785「票种规则明确允许退款」出厂默认关闭、逐票种开启；BSM:806 记录退款原因、责任主体、适用规则版本）。
 * 第十八批 AH 段第 256 条 256-R1：确认拨付填的原路退回结果落退款单、进成功留痕（BSM:806「原路退回结果」；PGM:899「结果是什么，都要能查到」）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TicketRefundReasonJdbcIntegrationTests {

    @Autowired private JdbcTicketingRepository repository;
    @Autowired private TicketRefundService refundService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MockMvc mockMvc;

    private final Instant now = Instant.now();
    private String suffix;
    private String orderId;
    private String instA;
    private String instB;
    private String entityId;

    @BeforeEach
    void seed() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        orderId = "o_tk05r1_" + suffix;
        instA = "inst_tk05r1_a_" + suffix;
        instB = "inst_tk05r1_b_" + suffix;
        entityId = "be_tk05r1_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)
                VALUES (?, '活动主办公司', 'project_company', 'active', now(), now())""", entityId);
        String ticketTypeId = "tt_tk05r1_" + suffix;
        // 票种出厂默认不可退（refundable=false）。
        repository.saveTicketType(new TicketTypeRecord(
                ticketTypeId, "不可退票种", "weekday_weekend_holiday",
                9900L, 11900L, 12900L, false, "published", null, null, now, now, null, 0, null, false, null,
                TicketTypeRecord.NO_COMBO_DISCOUNT));
        repository.saveTicketOrder(new TicketOrderRecord(
                orderId, "TO-" + suffix, null, ticketTypeId, null,
                2, java.time.LocalDate.now().plusDays(3), "miniapp", "13900000000",
                19800L, null, 19800L, 0L, "CNY", "paid", "issued", "created", now, now, null, now, null, null));
        String templateId = "tpl_tk05r1_" + suffix;
        repository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                templateId, "Entry", "entry", 1, 9900L, "active", null, null, null, null, now, now,
                null, null, null, null));
        repository.saveTicketInstance(new TicketInstanceRecord(
                instA, "TN-A-" + suffix, orderId, ticketTypeId, 1, 1, 1, null, null, "active", null, null, now, now));
        repository.saveTicketInstance(new TicketInstanceRecord(
                instB, "TN-B-" + suffix, orderId, ticketTypeId, 2, 1, 1, null, null, "active", null, null, now, now));
        repository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_tk05r1_a_" + suffix, "EA-" + suffix, ticketTypeId, orderId, templateId, "entry",
                null, null, null, 1, 0, 9900L, "active", now, now.plusSeconds(86400), now, now, 1, 9900L,
                null, null, null, null, instA, 1, null, null));
        // 实例 B 已核销（used_times=1）。
        repository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_tk05r1_b_" + suffix, "EB-" + suffix, ticketTypeId, orderId, templateId, "entry",
                null, null, null, 1, 1, 9900L, "active", now, now.plusSeconds(86400), now, now, 1, 9900L,
                null, null, null, null, instB, 1, null, null));
        TestSecurityActors.asPlatformAdmin("usr_tk05r1_requester");
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    /** 活动或场次取消：不看票种开关，不可退票种也可申请；记录理由码、责任主体与申请时票种开关快照。 */
    @Test
    void eventCancelledRefundAllowedOnNonRefundableType() {
        TicketRefundRecord refund = refundService.requestRefund(request("r1-ec", "event_cancelled", entityId, instA));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, reason_code, responsible_entity_id, ticket_type_refundable_snapshot FROM ticket_refunds WHERE id = ?",
                refund.id());
        assertThat(row).containsEntry("status", "requested")
                .containsEntry("reason_code", "event_cancelled")
                .containsEntry("responsible_entity_id", entityId)
                .containsEntry("ticket_type_refundable_snapshot", false);
    }

    /** 票种规则明确允许：仍须票种开关为允许（出厂默认关闭）。 */
    @Test
    void ticketTypeRuleReasonStillRequiresRefundableFlag() {
        assertThatThrownBy(() -> refundService.requestRefund(request("r1-ttr", "ticket_type_rule", null, instA)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("TICKET_TYPE_NOT_REFUNDABLE");
    }

    /** 缺理由码、闭集外理由、或从普通入口提交通票安全例外：一律拒。 */
    @Test
    void refundWithoutReasonCodeRejected() {
        for (String code : new String[] {null, "special_approval", "safety_exception"}) {
            assertThatThrownBy(() -> refundService.requestRefund(request("r1-none-" + code, code, entityId, instA)))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).code()).isEqualTo("REFUND_REASON_CODE_REQUIRED");
        }
    }

    /** BSM:806「系统完整记录退款原因、责任主体……」：每笔退款（含票种规则明确允许）都须写明责任主体（主体主数据中在用的法律主体）。 */
    @Test
    void everyReasonRequiresResponsibleEntity() {
        jdbcTemplate.update("UPDATE ticket_types SET refundable = TRUE WHERE id = ?", "tt_tk05r1_" + suffix);
        assertThatThrownBy(() -> refundService.requestRefund(request("r1-ttr-noent", "ticket_type_rule", null, instA)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("REFUND_RESPONSIBLE_ENTITY_REQUIRED");
        assertThatThrownBy(() -> refundService.requestRefund(request("r1-noent", "duplicate_charge", null, instA)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("REFUND_RESPONSIBLE_ENTITY_REQUIRED");
        assertThatThrownBy(() -> refundService.requestRefund(request("r1-badent", "duplicate_charge", "be_missing_" + suffix, instA)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("REFUND_RESPONSIBLE_ENTITY_REQUIRED");
    }

    /** 五种情形之外：只能由最高权限逐笔放行——非最高权限审批人拒，最高权限批准生效。 */
    @Test
    void masterOverrideRequiresMasterApprover() {
        TicketRefundRecord refund = refundService.requestRefund(request("r1-mo", "master_override", entityId, instA));

        TestSecurityActors.asPlatformAdmin("usr_tk05r1_approver");
        assertThatThrownBy(() -> refundService.approveRefund(refund.id(),
                new StatusChangeRequest("r1-mo-a1-" + suffix, "approve", "批准", List.of())))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("MASTER_AUTHORITY_REQUIRED");

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                        null, null, "active"), "token", List.of()));
        TicketRefundRecord approved = refundService.approveRefund(refund.id(),
                new StatusChangeRequest("r1-mo-a2-" + suffix, "approve", "逐笔放行", List.of()));
        assertThat(approved.status()).isEqualTo("approved");
        assertThat(approved.approvedBy()).isEqualTo(SessionIssuer.MASTER_USER_ID);
        assertThat(approved.reasonCode()).isEqualTo("master_override");
    }

    /** 最高权限逐笔放行不解除已开始核销后的退款禁止。 */
    @Test
    void masterOverrideCannotRefundVerifiedTicket() {
        assertThatThrownBy(() -> refundService.requestRefund(request("r1-mov", "master_override", entityId, instB)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("TICKET_INSTANCE_VERIFIED_NO_REFUND");
    }

    /**
     * 256-R1（BSM:806「系统完整记录……原路退回结果」；PGM:899「结果是什么，都要能查到」）：另一审批人批准后，财务按前端同键
     * POST confirm-payout {idempotency_key, reason} 填的原路退回结果写进退款单 payout_result、记入成功留痕，退款台账接口返回。
     */
    @Test
    void confirmPayoutRecordsPayoutResultOnRefundAuditAndList() throws Exception {
        TicketRefundRecord refund = refundService.requestRefund(request("r256-req", "event_cancelled", entityId, instA));
        TestSecurityActors.asPlatformAdmin("usr_r256_approver");
        assertThat(refundService.approveRefund(refund.id(),
                new StatusChangeRequest("r256-a-" + suffix, "approve", "审批通过", List.of())).status()).isEqualTo("approved");
        RequestPostProcessor finance = financeActor();
        String result = "微信原路退回成功，交易单号 W123";

        mockMvc.perform(post("/api/v1/ticketing/refunds/" + refund.id() + "/confirm-payout").with(finance)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"r256-payout-" + suffix + "\",\"reason\":\"" + result + "\"}"))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForMap("SELECT status, payout_result FROM ticket_refunds WHERE id = ?", refund.id()))
                .containsEntry("status", "success")
                .containsEntry("payout_result", result);
        assertThat(jdbcTemplate.queryForList("SELECT metadata->>'payout_result' FROM audit_events"
                + " WHERE event_name = 'ticket_refund.payout_confirmed' AND business_id = ?", String.class, refund.id()))
                .containsExactly(result);
        mockMvc.perform(get("/api/v1/ticketing/refunds").with(finance))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + refund.id() + "')].payout_result")
                        .value(org.hamcrest.Matchers.contains(result)));
    }

    /** 财务人员：角色带确认拨付与查看退款台账两个权限码，走真实授权链。 */
    private RequestPostProcessor financeActor() {
        String role = "role_r256_" + suffix;
        jdbcTemplate.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'退款确认拨付回归','project','active',now())",
                role, "退款确认拨付回归" + suffix);
        jdbcTemplate.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN('ticket.refund.approve','ticket.refund.view')", role);
        String user = "usr_r256_fin_" + suffix;
        jdbcTemplate.update("INSERT INTO access_users(id,username,account_type,organization_id,status,created_at) VALUES(?,?,'platform_admin','org','active',now())", user, user);
        jdbcTemplate.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        AccessActor actor = new AccessActor(user, user, "platform_admin", "org", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private TicketRefundRequest request(String key, String reasonCode, String responsibleEntityId, String instanceId) {
        return new TicketRefundRequest(key + "-" + suffix, "refund", List.of(), orderId, null, "活动取消退票", "original",
                null, List.of(instanceId), reasonCode, responsibleEntityId);
    }
}
