package com.complex.platform.settlement;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.amusement.AmusementBusinessDayTestFixture;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 N 段第 124 条 124-R4（PGM L738「访问被拒绝也要留痕，便于排查是权限问题还是开关问题。」；PGM L966「所有有过程的业务动作都必须留痕」；
 * 先例 18I 第 90 条）：结算配置、开票清单、结算单过账、退款授权配置与能源费结算写口任一步失败（权限、服务层校验、状态冲突）落 OpenAPI 声明的
 * x-audit-event-failure；结算单过账成功落 settlement.bill.posted。请求按前端蛇形键、走真实授权链；失败事件独立事务、不随测试回滚，按本次幂等键或业务 id 查。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SettlementWriteFailureAuditJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    private String project;

    @BeforeEach
    void seed() {
        project = AmusementBusinessDayTestFixture.complex(jdbc);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 渠道费用缺 fee_layer（服务层校验）→ 400，落 settlement.config.channel_fee_configs.update_failed。 */
    @Test
    void channelFeeWithoutFeeLayerRecordsUpdateFailure() throws Exception {
        String key = "r4_cf_" + suffix;
        send("/api/v1/settlement/config/channel-fee", staff("r4_cf_" + suffix, "settlement.config.manage"),
                "{\"idempotency_key\":\"" + key + "\",\"channel\":\"wechat_pay\",\"fee_bps\":60}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));

        assertThat(failuresByKey("settlement.config.channel_fee_configs.update_failed", key))
                .containsExactly("channel_fee_configs|settlement.config.manage|VALIDATION_FAILED");
    }

    /** 无 settlement.config.manage 改分配方式 → 403，权限拒绝也落 settlement.config.ticket_distribution_configs.update_failed。 */
    @Test
    void distributionMethodWithoutManagePermissionRecordsUpdateFailure() throws Exception {
        String key = "r4_dm_" + suffix;
        send("/api/v1/settlement/config/distribution-method", staff("r4_dm_" + suffix, "settlement.config.view"),
                "{\"idempotency_key\":\"" + key + "\",\"method_type\":\"by_rank_tier\",\"special_approval_term_value\":3,"
                        + "\"special_approval_term_unit\":\"month\",\"special_approval_term_min_value\":1,\"special_approval_term_max_value\":6}")
                .andExpect(status().isForbidden());

        assertThat(failuresByKey("settlement.config.ticket_distribution_configs.update_failed", key))
                .containsExactly("ticket_distribution_configs|settlement.config.manage|PERMISSION_DENIED");
    }

    /** 能源费结算账期非法（服务层判）→ 409，落 energy.fee_settle_failed。 */
    @Test
    void energyFeeSettleWithInvalidPeriodRecordsSettleFailure() throws Exception {
        String key = "r4_ef_" + suffix;
        send("/api/v1/energy/settle-energy-fee", staff("r4_ef_" + suffix, "energy.reading.manage"),
                "{\"idempotency_key\":\"" + key + "\",\"period\":\"2026-13\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ENERGY_CONFIRMATION_REQUIRED"));

        assertThat(failuresByKey("energy.fee_settle_failed", key))
                .containsExactly("energy_fee|energy.reading.manage|ENERGY_CONFIRMATION_REQUIRED");
    }

    /** 无 refund.authority.manage 改退款额度 → 403，落 refund_authority.change_failed。 */
    @Test
    void refundLimitWithoutManagePermissionRecordsChangeFailure() throws Exception {
        String key = "r4_rl_" + suffix;
        send("/api/v1/finance/refund-authority/limits", staff("r4_rl_" + suffix, "refund.authority.view"),
                "{\"idempotency_key\":\"" + key + "\",\"subject_type\":\"staff\",\"subject_id\":\"r4_op_" + suffix
                        + "\",\"refund_domain\":\"ticket\",\"limit_amount_cent\":50000,\"status\":\"active\"}")
                .andExpect(status().isForbidden());

        assertThat(failuresByKey("refund_authority.change_failed", key))
                .containsExactly("refund_authority_limit|refund.authority.manage|PERMISSION_DENIED");
    }

    /** 开票清单条目不存在时流转状态 → 404，落 settlement.invoice.status_change_failed（按条目 id 查）。 */
    @Test
    void invoiceStatusChangeOnMissingItemRecordsStatusChangeFailure() throws Exception {
        String itemId = "r4_inv_" + suffix;
        send("/api/v1/settlement/invoice-items/" + itemId + "/status", staff("r4_inv_" + suffix, "settlement.invoice.manage"),
                "{\"status\":\"issued\",\"invoice_no\":\"FP-1\",\"remark\":null}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("INVOICE_ITEM_NOT_FOUND"));

        assertThat(failuresById("settlement.invoice.status_change_failed", itemId))
                .containsExactly("settlement_invoice_item|settlement.invoice.manage|INVOICE_ITEM_NOT_FOUND");
    }

    /** 已确认结算单人工过账 → 200，落 settlement.bill.posted 一行（business_id＝结算单 id）。 */
    @Test
    void postingConfirmedBillRecordsPosted() throws Exception {
        String bill = bill("confirmed");
        send("/api/v1/settlement/bills/" + bill + "/post", staff("r4_bp_" + suffix, "settlement.bill.post"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));

        assertThat(jdbc.queryForList("""
                SELECT business_type || '|' || permission_code FROM audit_events
                 WHERE event_name = 'settlement.bill.posted' AND event_result = 'success' AND business_id = ?""", String.class, bill))
                .containsExactly("settlement_bill|settlement.bill.post");
    }

    /** 草稿结算单过账 → 409，落 settlement.bill.post_failed（按结算单 id 查）。 */
    @Test
    void postingDraftBillRecordsPostFailure() throws Exception {
        String bill = bill("draft");
        send("/api/v1/settlement/bills/" + bill + "/post", staff("r4_bd_" + suffix, "settlement.bill.post"), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("SETTLEMENT_BILL_NOT_CONFIRMED"));

        assertThat(failuresById("settlement.bill.post_failed", bill))
                .containsExactly("settlement_bill|settlement.bill.post|SETTLEMENT_BILL_NOT_CONFIRMED");
    }

    /**
     * 第十八批 AG 段 259-R3（OpenAPI 与代码一致：综合服务费草案请求体补 ticket_type_id、monthly_cap_amount_cent）：按蛇形键带票种与月封顶 → 200，
     * 草案行落所传票种与封顶；票种不存在 → 400，不落草案。
     */
    @Test
    void comprehensiveServiceFeeDraftKeepsTicketTypeAndMonthlyCap() throws Exception {
        String ticketType = "r3_tt_" + suffix;
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)"
                + " VALUES (?, '综合服务费草案票种', 'fixed_price', FALSE, 'active', now(), now())", ticketType);
        RequestPostProcessor manager = staff("r3_csf_" + suffix, "settlement.config.manage");
        String body = "{\"idempotency_key\":\"%s\",\"project_id\":\"" + project + "\",\"ticket_type_id\":\"%s\",\"fee_bps\":1000,"
                + "\"monthly_cap_amount_cent\":5000}";

        send("/api/v1/settlement/config/comprehensive-service-fee", manager, body.formatted("r3_csf_bad_" + suffix, "r3_tt_missing_" + suffix))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        send("/api/v1/settlement/config/comprehensive-service-fee", manager, body.formatted("r3_csf_ok_" + suffix, ticketType))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForList("""
                SELECT ticket_type_id || '|' || monthly_cap_amount_cent FROM comprehensive_service_fee_configs
                 WHERE project_id = ? AND is_draft = TRUE AND status = 'active'""", String.class, project))
                .containsExactly(ticketType + "|5000");
    }

    private String bill(String status) {
        String id = "r4_sb_" + status + "_" + suffix;
        jdbc.update("""
                INSERT INTO settlement_bills (id, project_id, period_start, period_end, status, created_at, updated_at)
                VALUES (?, ?, DATE '2001-01-01', DATE '2001-01-31', ?, now(), now())""", id, project, status);
        return id;
    }

    private List<String> failuresByKey(String event, String key) {
        return jdbc.queryForList("""
                SELECT business_type || '|' || permission_code || '|' || (metadata->>'error_code') FROM audit_events
                 WHERE event_name = ? AND event_result = 'failure' AND idempotency_key = ?""", String.class, event, key);
    }

    private List<String> failuresById(String event, String businessId) {
        return jdbc.queryForList("""
                SELECT business_type || '|' || permission_code || '|' || (metadata->>'error_code') FROM audit_events
                 WHERE event_name = ? AND event_result = 'failure' AND business_id = ?""", String.class, event, businessId);
    }

    private ResultActions send(String path, RequestPostProcessor who, String body) throws Exception {
        var request = post(path).with(who).contentType(MediaType.APPLICATION_JSON);
        return mockMvc.perform(body == null ? request : request.content(body));
    }

    /** 平台域员工账号（无商户归属），经一个中文名角色持有给定权限码（后台 Web 终端）。 */
    private RequestPostProcessor staff(String id, String... permissions) {
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,status,created_at) VALUES(?,?,'staff',?,'active',now())",
                id, id, project);
        String role = "r4_role_" + id;
        jdbc.update("INSERT INTO roles(id,role_name,created_at) VALUES(?,?,now())", role, "写口失败留痕测试角色-" + id);
        for (String permission : permissions) {
            jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)", role, permission);
        }
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", id, role);
        AccessActor actor = new AccessActor(id, id, "staff", project, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of())).postProcessRequest(request);
        };
    }
}
