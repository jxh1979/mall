package com.complex.platform.analytics;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.ticketing.IssuedEntitlementRecord;
import com.complex.platform.ticketing.TicketCredentialRecord;
import com.complex.platform.ticketing.TicketOrderRecord;
import com.complex.platform.ticketing.TicketingRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 第十八批 Q 段第 147 条 147-R1（BSM:1820「核销：权益发放数、核销数、核销率、拒绝数。」；ENUM:43「拒绝（规则不允许）、失败（设备或网络原因）」
 * 两态分列）：持常设「执行核销」的本商户员工按蛇形键经真实入口核销——过期权益 409（落 rejected/entitlement_expired）两笔、
 * 过刷新窗口的动态码 200（failed/window_expired）、有效票码 200（success）；其中一笔拒绝的发生时刻挪到当日营业日窗之前。
 * 刷新经营看板快照后：本商户 verify.rejected＝1（失败不计入、窗前那笔不计入），平台段增 1，商户分项目段 1，verify.done 仍是 1；
 * 快照列表接口返回该指标行。修复前快照里没有 verify.rejected，第一处断言即红。
 *
 * <p>147-R2（BSM:1820 同一句「权益发放数」）：本商户当日发放两条权益、一笔核销成功，刷新后 verify.issued＝2（商户、商户分项目段，
 * 平台段增 2），核销率＝核销数/发放数＝0.5；把其中一条的发放时刻挪到当日营业日窗之前，当日 verify.issued 减为 1、核销率 1。
 *
 * <p>不开测试事务：规则拒绝的核销记录在请求事务结束后另起事务写入（TicketVerificationService.recordRuleRejection），
 * 权益等夹具须真提交才写得进。快照刷新与读取放在回滚事务里，不在共享库留下快照行；核销夹具在清场时删除。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class VerificationRejectedMetricJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TicketingRepository ticketing;
    @Autowired private AnalyticsRepository repository;
    @Autowired private BusinessDayResolver businessDayResolver;
    @Autowired private PlatformTransactionManager transactionManager;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "vrm_p_" + suffix;
    private final String merchant = "vrm_m_" + suffix;
    private final String staff = "usr_vrm_staff_" + suffix;
    private final String role = "role_vrm_staff_" + suffix;
    private final String code = "vrm-code-" + suffix;
    private final String device = "device_vrm_" + suffix;

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        String entitlement = "ent_" + code;
        jdbc.update("DELETE FROM entitlement_day_activations WHERE entitlement_id = ?", entitlement);
        com.complex.platform.support.AppendOnlyTestBypass.update(
                jdbc, "DELETE FROM settlement_transactions WHERE entitlement_id = ?", entitlement);
        jdbc.update("DELETE FROM ticket_verifications WHERE entitlement_id = ?", entitlement);
        jdbc.update("DELETE FROM ticket_credentials WHERE entitlement_id = ?", entitlement);
        jdbc.update("DELETE FROM issued_entitlements WHERE id = ?", entitlement);
        jdbc.update("DELETE FROM issued_entitlements WHERE id = ?", "ent2_" + code);
        jdbc.update("DELETE FROM ticket_orders WHERE id = ?", "order_" + code);
        jdbc.update("DELETE FROM entitlement_templates WHERE id = ?", "et_" + code);
        jdbc.update("DELETE FROM ticket_types WHERE id = ?", "tt_" + code);
        jdbc.update("DELETE FROM terminal_devices WHERE id = ?", device);
        jdbc.update("DELETE FROM user_roles WHERE user_id = ?", staff);
        jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", role);
        jdbc.update("DELETE FROM roles WHERE id = ?", role);
        jdbc.update("DELETE FROM access_users WHERE id = ?", staff);
        jdbc.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", "vrm-%" + suffix);
        jdbc.update("DELETE FROM merchants WHERE id = ?", merchant);
        jdbc.update("DELETE FROM projects WHERE id = ?", project);
    }

    @Test
    void rejectedVerificationsAreCountedPerBusinessDayWithoutFailures() throws Exception {
        LocalDate d = businessDayResolver.businessDate(null, null, Instant.now());
        BigDecimal platformBefore = inRollback(() -> {
            new DashboardAggregationJob(repository, businessDayResolver, false).refreshSnapshots(d);
            return nonNull(repository.readMetricSnapshots(d, null).get("verify.rejected"));
        });
        seed();
        String entitlement = "ent_" + code;

        // 两笔规则拒绝（权益已过期）：409，各落一条 rejected 核销记录；第一笔挪到当日营业日窗之前（前一营业日发生）。
        jdbc.update("UPDATE issued_entitlements SET valid_to = now() - interval '1 hour' WHERE id = ?", entitlement);
        verify("vrm-rejected-early-" + suffix, code).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ENTITLEMENT_EXPIRED"));
        jdbc.update("UPDATE ticket_verifications SET created_at = ? WHERE entitlement_id = ? AND status = 'rejected'",
                Timestamp.from(businessDayResolver.start(null, null, d).minusSeconds(60)), entitlement);
        verify("vrm-rejected-" + suffix, code).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ENTITLEMENT_EXPIRED"));
        jdbc.update("UPDATE issued_entitlements SET valid_to = now() + interval '10 days' WHERE id = ?", entitlement);

        // 技术失败（过刷新窗口的动态码）：200、failed/window_expired——ENUM:43 失败与拒绝分列，不计入拒绝数。
        String expiredToken = "cred_vrm_expired_" + suffix;
        Instant now = Instant.now();
        ticketing.saveTicketCredential(new TicketCredentialRecord(
                "tcred_expired_" + code, entitlement, "order_" + code, expiredToken, "nonce_" + code, null, "dynamic_qr",
                now.minusSeconds(600), now.plusSeconds(3600), "active", 1, now, now, 60,
                now.minusSeconds(600), "not-checked-window-expired", "v1"));
        verify("vrm-failed-" + suffix, expiredToken).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("failed"))
                .andExpect(jsonPath("$.data.result_reason").value("window_expired"));

        // 有效票码核销成功。
        verify("vrm-success-" + suffix, code).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("success"));
        assertThat(jdbc.queryForList(
                "SELECT status || '/' || coalesce(result_reason, '') FROM ticket_verifications WHERE entitlement_id = ? ORDER BY created_at",
                String.class, entitlement)).containsExactly(
                "rejected/entitlement_expired", "rejected/entitlement_expired", "failed/window_expired", "success/");

        inRollback(() -> {
            new DashboardAggregationJob(repository, businessDayResolver, false).refreshSnapshots(d);
            Map<String, BigDecimal> mine = repository.readMetricSnapshots(d, merchant);
            assertThat(mine.get("verify.rejected")).isEqualByComparingTo("1");
            assertThat(mine.get("verify.done")).isEqualByComparingTo("1");
            assertThat(nonNull(repository.readMetricSnapshots(d, null).get("verify.rejected")).subtract(platformBefore))
                    .isEqualByComparingTo("1");
            try {
                List<Map<String, Object>> rows = JsonPath.read(mockMvc.perform(get("/api/v1/analytics/business-metric-snapshots")
                                .with(platformAdmin()))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.items[*]");
                assertThat(rows.stream()
                        .filter(r -> "verify.rejected".equals(r.get("metric_key")) && d.toString().equals(r.get("business_date"))
                                && r.get("segment_id") != null && r.get("segment_id").toString().startsWith(merchant))
                        .map(r -> r.get("segment_type") + ":" + r.get("segment_id") + "=" + new BigDecimal(r.get("actual_value").toString()).stripTrailingZeros().toPlainString())
                        .toList()).containsExactlyInAnyOrder("merchant:" + merchant + "=1", "merchant_project:" + merchant + "|" + project + "=1");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return null;
        });
    }

    @Test
    void issuedEntitlementsAreCountedPerBusinessDay() throws Exception {
        LocalDate d = businessDayResolver.businessDate(null, null, Instant.now());
        BigDecimal platformBefore = inRollback(() -> {
            new DashboardAggregationJob(repository, businessDayResolver, false).refreshSnapshots(d);
            return nonNull(repository.readMetricSnapshots(d, null).get("verify.issued"));
        });
        seed();
        // 同订单、同商户当日再发一条权益（不核销）。
        Instant now = Instant.now();
        String second = "ent2_" + code;
        ticketing.saveIssuedEntitlement(new IssuedEntitlementRecord(
                second, code + "-2", "tt_" + code, "order_" + code, "et_" + code, "bar_drink",
                merchant, "venue_vrm_" + suffix, "point_" + suffix, 1, 0, 9999L, "active",
                now.minusSeconds(864000), now.plusSeconds(864000), now, now, 1, null,
                "virtual", 5500L, "per_use", null, null, 0, null, null));
        verify("vrm-issued-success-" + suffix, code).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("success"));

        assertIssued(d, platformBefore, "2", "0.5");
        jdbc.update("UPDATE issued_entitlements SET created_at = ? WHERE id = ?",
                Timestamp.from(businessDayResolver.start(null, null, d).minusSeconds(60)), second);
        assertIssued(d, platformBefore, "1", "1");
    }

    private void assertIssued(LocalDate d, BigDecimal platformBefore, String issued, String rate) {
        inRollback(() -> {
            new DashboardAggregationJob(repository, businessDayResolver, false).refreshSnapshots(d);
            Map<String, BigDecimal> mine = repository.readMetricSnapshots(d, merchant);
            assertThat(mine.get("verify.issued")).isEqualByComparingTo(issued);
            assertThat(mine.get("verify.done")).isEqualByComparingTo("1");
            assertThat(mine.get("verify.redemption_rate")).isEqualByComparingTo(rate);
            assertThat(nonNull(repository.readMetricSnapshots(d, null).get("verify.issued")).subtract(platformBefore))
                    .isEqualByComparingTo(issued);
            assertThat(jdbc.queryForObject("""
                    SELECT actual_value FROM analytics_business_metric_snapshots
                     WHERE metric_key = 'verify.issued' AND business_date = ? AND period_type = 'daily'
                       AND segment_type = 'merchant_project' AND segment_id = ?
                    """, BigDecimal.class, d, merchant + "|" + project)).isEqualByComparingTo(issued);
            return null;
        });
    }

    private <T> T inRollback(Supplier<T> body) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            status.setRollbackOnly();
            return body.get();
        });
    }

    private static BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private org.springframework.test.web.servlet.ResultActions verify(String key, String credential) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/verifications").with(session(staff, "staff", project, merchant))
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "%s", "credential_type": "qr", "credential_payload": "%s",
                         "device_id": "%s", "point_id": "point_%s", "business_context": {}}
                        """.formatted(key, credential, device, suffix)));
    }

    // 平台管理员（快照列表入口权限另给一个明确的角色），建在回滚事务里随之撤掉。
    private RequestPostProcessor platformAdmin() {
        String admin = "usr_vrm_admin_" + suffix;
        String adminRole = "role_vrm_admin_" + suffix;
        account(admin, "platform_admin", "org", null, adminRole, "核销拒绝数回归管理员" + suffix, "analytics.dashboard.view");
        return session(admin, "platform_admin", "org", null);
    }

    private void account(String userId, String accountType, String organizationId, String merchantId,
                         String roleId, String roleName, String permission) {
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                + " VALUES (?, ?, '第147条核销拒绝数回归', 'project', 'active', now())", roleId, roleName);
        jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, permission);
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, ?, ?, ?, 'active', now())", userId, userId, accountType, organizationId, merchantId);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
    }

    // 后台 Web 终端 + 生产过滤器放入的 AccessActor 主体（与账号行一致）。
    private static RequestPostProcessor session(String userId, String accountType, String organizationId, String merchantId) {
        AccessActor actor = new AccessActor(userId, userId, accountType, organizationId, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    // 本项目、本商户的普通单次酒水权益（有效期前后各 10 天），订单锁定本项目；常设「执行核销」的本商户员工；已登记的核销设备。
    private void seed() {
        Instant now = Instant.now();
        Timestamp ts = Timestamp.from(now);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','核销拒绝数项目','active',now(),now())",
                project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '核销拒绝数商户', 'bar', 'active', now(), now(), ?)", merchant, project);
        account(staff, "staff", project, merchant, role, "核销拒绝数回归核销员" + suffix, "verification.execute");
        jdbc.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, status, created_at, updated_at)
                VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, ?)
                """, device, "dev-" + device, "DC-" + device, ts, ts);
        String typeId = "tt_" + code;
        String templateId = "et_" + code;
        String venueId = "venue_vrm_" + suffix;
        jdbc.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)
                VALUES (?, '核销拒绝数票种', 'fixed', FALSE, 'active', ?, ?)
                """, typeId, ts, ts);
        jdbc.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status, venue_id,
                    entitlement_kind, agreement_price_cent, consumption_window, created_at, updated_at)
                VALUES (?, '单次酒水', 'bar_drink', 1, 0, 'active', ?, 'virtual', 5500, 'per_use', ?, ?)
                """, templateId, venueId, ts, ts);
        ticketing.saveTicketOrder(new TicketOrderRecord(
                "order_" + code, "ON-" + code, "cust_" + code, typeId, null, 1,
                LocalDate.now(), "web", null, 0L, null, 0L, 0L, "CNY", "paid", "issued", "created", now, now, null, null, null, project));
        ticketing.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "bar_drink",
                merchant, venueId, "point_" + suffix, 1, 0, 9999L, "active",
                now.minusSeconds(864000), now.plusSeconds(864000), now, now, 1, null,
                "virtual", 5500L, "per_use", null, null, 0, null, null));
    }
}
